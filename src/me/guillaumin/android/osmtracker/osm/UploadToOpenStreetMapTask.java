package me.guillaumin.android.osmtracker.osm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.model.Track.OSMVisibility;
import me.guillaumin.android.osmtracker.osm.ProgressMultipartEntity.ProgressListener;
import me.guillaumin.android.osmtracker.util.DialogUtils;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Uploads a GPX file to OpenStreetMap
 * 
 * @author Nicolas Guillaumin
 */
public class UploadToOpenStreetMapTask extends AsyncTask<Void, Void, Void> {

	private static final String TAG = UploadToOpenStreetMapTask.class.getSimpleName();

	private static final String GPX_MIMETYPE = "application/gpx+xml";
	
	/** Upload progress dialog */
	private ProgressDialog dialog;
	
	private final Activity activity;
	private final long trackId;
	
	/** OAuth consumer to sign the post request */
	private final CommonsHttpOAuthConsumer oAuthConsumer;
	
	/** File to export */
	private final File gpxFile;
	
	/** Filename to use when uploading */
	private final String filename;
	
	/** Track description */
	private final String description;
	
	/** Track tags */
	private final String tags;
	
	/** Track visibility */
	private final OSMVisibility visibility;

	/**
	 * Error message, or text of the response returned by OSM
	 * if the request completed
	 */
	private String errorMsg;

	/**
	 * Either the HTTP result code, or -1 for an internal error
	 */
	private int resultCode = -1;
	
	private HttpPost request;
	private HttpResponse response;
	
	public UploadToOpenStreetMapTask(Activity activity,
			long trackId, CommonsHttpOAuthConsumer oAuthConsumer,
			File gpxFile, String filename,
			String description, String tags, OSMVisibility visibility) {
		this.activity = activity;
		this.trackId = trackId;
		this.filename = filename;
		
		this.oAuthConsumer = oAuthConsumer;
		this.gpxFile = gpxFile;
		this.description = (description == null) ? "test" : description;
		this.tags = (tags == null) ? "test" : tags;
		this.visibility = (visibility == null) ? OSMVisibility.Private : visibility;
	}
	
	@Override
	protected void onPreExecute() {
		try {
			// Prepare and OAuth-sign the request request
			request = new HttpPost(OpenStreetMapConstants.Api.Gpx.CREATE);
			oAuthConsumer.sign(request);
			
			final long totalSize = gpxFile.length();
			
			// Custom entity to display a progress bar while uploading
			MultipartEntity entity = new ProgressMultipartEntity(
					HttpMultipartMode.BROWSER_COMPATIBLE,
					Charset.defaultCharset(),
					new ProgressListener() {
				@Override
				public void transferred(long num) {
					dialog.incrementProgressBy((int) num);
					if (num >= totalSize) {
						// Finish sending. Switch to an indeterminate progress
						// dialog while the OSM server processes the request
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
						 		dialog.setIndeterminate(true);
						 		dialog.setCancelable(false);
								dialog.setTitle(activity.getResources().getString(R.string.osm_upload_waiting_response));
							}
						});
					}
					
				}
			});

			// API parameters
			entity.addPart(OpenStreetMapConstants.Api.Gpx.Parameters.FILE, new FileBody(gpxFile, filename, GPX_MIMETYPE, Charset.defaultCharset().name()));
			entity.addPart(OpenStreetMapConstants.Api.Gpx.Parameters.DESCRIPTION, new StringBody(description, Charset.defaultCharset()));
			entity.addPart(OpenStreetMapConstants.Api.Gpx.Parameters.TAGS, new StringBody(tags, Charset.defaultCharset()));
			entity.addPart(OpenStreetMapConstants.Api.Gpx.Parameters.VISIBILITY, new StringBody(visibility.toString().toLowerCase(),Charset.defaultCharset()));
			request.setEntity(entity);
				
			// Display progress dialog
			dialog = new ProgressDialog(activity);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(false);
			dialog.setMax((int) totalSize);
			dialog.setTitle(
					activity.getResources().getString(R.string.osm_upload_sending)
					.replace("{0}", Long.toString(trackId)));
			dialog.setCancelable(false);
			dialog.show();

		} catch (Exception e) {
			Log.e(TAG, "onPreExecute() failed", e);
			errorMsg = e.getLocalizedMessage();
			cancel(true);
		}
	}
	
	@Override
	protected void onPostExecute(Void result) {
		switch (resultCode) {
		case -1:
			dialog.dismiss();
			// Internal error, the request didn't start at all
			DialogUtils.showErrorDialog(activity,
					activity.getResources().getString(R.string.osm_upload_error)
						+ ": " + errorMsg);
			break;
		case HttpStatus.SC_OK:
			dialog.dismiss();
			// Success ! Update database and close activity
			DataHelper.setTrackUploadDate(trackId, System.currentTimeMillis(), activity.getContentResolver());
			
			new AlertDialog.Builder(activity)
				.setTitle(android.R.string.dialog_alert_title)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(R.string.osm_upload_sucess)
				.setCancelable(true)
				.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						activity.finish();
					}
				}).create().show();
			
			break;
		case HttpStatus.SC_UNAUTHORIZED:
			dialog.dismiss();
			// Authorization issue. Provide a way to clear credentials
			new AlertDialog.Builder(activity)
					.setTitle(android.R.string.dialog_alert_title)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(R.string.osm_upload_unauthorized)
					.setCancelable(true)
					.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();								
						}
					})
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
							editor.remove(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN);
							editor.remove(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET);
							editor.commit();

							dialog.dismiss();
						}
					}).create().show();

		default:
			// Another error. Display OSM response
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(response.getEntity().getContent())));
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ( (line = reader.readLine()) != null) {
					sb.append(line).append(System.getProperty("line.separator"));
				}
				
				dialog.dismiss();
				
				DialogUtils.showErrorDialog(activity,
						activity.getResources().getString(R.string.osm_upload_bad_response)
							.replace("{0}", Integer.toString(resultCode))
							.replace("{1}", sb.toString()));
			} catch (IOException ioe) {
				DialogUtils.showErrorDialog(activity, activity.getResources().getString(R.string.osm_upload_error) + ": " + ioe);
			} finally {
				if (dialog.isShowing()) {
					dialog.dismiss();
				}
				
				if (reader != null) {
					try { reader.close(); }
					catch (IOException ioe) { }
				}
			}
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		try {
			// Post request and get response code
			DefaultHttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(request);
			resultCode = response.getStatusLine().getStatusCode();
		} catch (Exception e) {
			Log.e(TAG, "doInBackground failed", e);
			errorMsg = e.getLocalizedMessage();
		}
		
		return null;
	}
}
