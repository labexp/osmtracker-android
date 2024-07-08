package net.osmtracker.osm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.model.Track;
import net.osmtracker.util.DialogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.common.errors.OsmAuthorizationException;
import de.westnordost.osmapi.common.errors.OsmBadUserInputException;
import de.westnordost.osmapi.traces.GpsTraceDetails;
import de.westnordost.osmapi.traces.GpsTracesApi;

/**
 * Uploads a GPX file to OpenStreetMap
 * 
 * @author Nicolas Guillaumin
 */
public class UploadToOpenStreetMapTask extends AsyncTask<Void, Void, Void> {

	private static final String TAG = UploadToOpenStreetMapTask.class.getSimpleName();
	
	/** Upload progress dialog */
	private ProgressDialog dialog;
	
	private final Activity activity;
	private final String accessToken;
	private final long trackId;

	/** File to export */
	private final File gpxFile;
	
	/** Filename to use when uploading */
	private final String filename;
	
	/** Track description */
	private final String description;
	
	/** Track tags */
	private final String tags;
	
	/** Track visibility */
	private final Track.OSMVisibility visibility;

	/**
	 * Error message, or text of the response returned by OSM
	 * if the request completed
	 */
	private String errorMsg;

	/**
	 * Either the HTTP result code, or -1 for an internal error
	 */
	private int resultCode = -1;
	private final int authorizationErrorResultCode = -2;
	private final int anotherErrorResultCode = -3;
	private final int okResultCode = 1;

	
	public UploadToOpenStreetMapTask(Activity activity, String accessToken, long trackId,
									 File gpxFile, String filename, String description, String tags,
									 Track.OSMVisibility visibility) {
		this.activity = activity;
		this.accessToken = accessToken;
		this.trackId = trackId;
		this.filename = filename;
		this.gpxFile = gpxFile;
		this.description = (description == null) ? "test" : description;
		this.tags = (tags == null) ? "test" : tags;
		this.visibility = (visibility == null) ? Track.OSMVisibility.Private : visibility;
	}
	
	@Override
	protected void onPreExecute() {
		try {
			// Display progress dialog
			dialog = new ProgressDialog(activity);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setIndeterminate(true);
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
		case okResultCode:
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
		case authorizationErrorResultCode:
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
							editor.remove(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN);
							editor.commit();

							dialog.dismiss();
						}
					}).create().show();

		default:
			// Another error. Display OSM response
			dialog.dismiss();
			// Internal error, the request didn't start at all
			DialogUtils.showErrorDialog(activity,
					activity.getResources().getString(R.string.osm_upload_error)
							+ ": " + errorMsg);
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		OsmConnection osm = new OsmConnection(OpenStreetMapConstants.Api.OSM_API_URL_PATH,
				OpenStreetMapConstants.OAuth2.USER_AGENT, accessToken);

		List<String> tags = new ArrayList<>();
		tags.add(this.tags);
		try (InputStream is = new FileInputStream(gpxFile)) {
			long gpxAPI = new GpsTracesApi(osm).create(filename, getVisibilityForOsmapi(visibility),
					description, tags, is);
			Log.v(TAG, "Gpx file uploaded. GPX id: " + gpxAPI);
			resultCode = okResultCode;
		} catch (IOException | IllegalArgumentException | OsmBadUserInputException e) {
			Log.d(TAG, e.getMessage());
			resultCode = -1; //internal error.
		} catch (OsmAuthorizationException oae) {
			Log.d(TAG, "OsmAuthorizationException");
			resultCode = authorizationErrorResultCode;
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			resultCode = anotherErrorResultCode;
		}
		return null;
	}


	private GpsTraceDetails.Visibility getVisibilityForOsmapi(Track.OSMVisibility visibility) {
		switch (visibility) {
			case Private: return GpsTraceDetails.Visibility.PRIVATE;
			case Public: return GpsTraceDetails.Visibility.PUBLIC;
			case Trackable: return GpsTraceDetails.Visibility.TRACKABLE;
			case Identifiable: return GpsTraceDetails.Visibility.IDENTIFIABLE;
		}
		return null;
	}
}
