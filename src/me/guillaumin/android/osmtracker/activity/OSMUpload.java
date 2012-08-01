package me.guillaumin.android.osmtracker.activity;

import java.io.File;
import java.util.concurrent.ExecutionException;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.model.Track;
import me.guillaumin.android.osmtracker.gpx.ExportToTempFileTask;
import me.guillaumin.android.osmtracker.osm.OSMConstants;
import me.guillaumin.android.osmtracker.osm.UploadToOsmTask;
import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

/**
 * Uploads a track on OSM using the API and
 * OAuth authentication.
 *
 * @author Nicolas Guillaumin
 */
public class OSMUpload extends TrackDetailEditor {

	private static final String TAG = OSMUpload.class.getSimpleName();
	
	/** URL that the browse will call once the user is authenticated */
	private static final String CALLBACK_URL = "osmtracker://osm-upload/?"+Schema.COL_TRACK_ID+"=";
	
	private static final CommonsHttpOAuthProvider oAuthProvider = new CommonsHttpOAuthProvider(
			OSMConstants.OAuth.Urls.REQUEST_TOKEN_URL,
			OSMConstants.OAuth.Urls.ACCESS_TOKEN_URL,
			OSMConstants.OAuth.Urls.AUTHORIZE_TOKEN_URL);
	private static final CommonsHttpOAuthConsumer oAuthConsumer = new CommonsHttpOAuthConsumer(
			OSMConstants.OAuth.CONSUMER_KEY,
			OSMConstants.OAuth.CONSUMER_SECRET);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.osm_upload, getTrackId());

		final Button btnOk = (Button) findViewById(R.id.osm_upload_btn_ok);
		btnOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveAndUpload();			
			}
		});
				
		final Button btnCancel = (Button) findViewById(R.id.osm_upload_btn_cancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Just close the dialog
				finish();				
			}
		});
		
		// Do not show soft keyboard by default
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}
	
	/**
	 * Gets the track ID we were called with, either from the
	 * intent extras if we were started by OSMTracker, or in the
	 * URI if we are returning from the browser.
	 * @return
	 */
	private long getTrackId() {
		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(Schema.COL_TRACK_ID)) {
			return getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		} else if (getIntent().getData().toString().startsWith(CALLBACK_URL)) {
			return Long.parseLong(getIntent().getData().getQueryParameter(Schema.COL_TRACK_ID));
		} else {
			throw new IllegalArgumentException("Missing Track ID");
		}
	}
	
	/**
	 * Will be called as well when we come back from the browser
	 * after user authentication.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		Cursor cursor = managedQuery(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
				null, null, null, null);
			
			if (! cursor.moveToFirst())	{
				// This shouldn't occur, it's here just in case.
				// So, don't make each language translate/localize it.
				Toast.makeText(this, "Track ID not found.", Toast.LENGTH_SHORT).show();
				finish();
				return;  // <--- Early return ---
			}

		bindTrack(Track.build(trackId, cursor, getContentResolver(), false));
		
		Uri uri = getIntent().getData();
		Log.d(TAG, "URI: " + uri);
		if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {
			// User is returning from authentication
			String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
			try {
				oAuthProvider.retrieveAccessToken(oAuthConsumer, verifier);
				
				// Store token in preferences for future use
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = prefs.edit();
				editor.putString(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN, oAuthConsumer.getToken());
				editor.putString(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET, oAuthConsumer.getTokenSecret());
				editor.commit();

				uploadToOsm();
				
			} catch (OAuthException oe) {
				showErrorDialog(getResources().getString(R.string.osm_upload_oauth_failed) + ": " + oe);
			}

		}
		

	}

	private void saveAndUpload() {
		save();
		
		try {
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			if ( prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN)
					&& prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET)) {
				oAuthConsumer.setTokenWithSecret(
						prefs.getString(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN, ""),
						prefs.getString(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET, ""));
				uploadToOsm();
			} else {
				String requestTokenUrl = oAuthProvider.retrieveRequestToken(oAuthConsumer, CALLBACK_URL+trackId);
				
				// Open browser for user to authenticate
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestTokenUrl)));
			}
		} catch (OAuthException oe) {
			showErrorDialog(getResources().getString(R.string.osm_upload_oauth_failed) + ": " + oe);
		}
	}
	
	private void uploadToOsm() {
		try {
			ExportToTempFileTask task = new ExportToTempFileTask(this, trackId);
			task.execute().get();
			File f = task.getTmpFile();
		
			int result = new UploadToOsmTask(oAuthConsumer, f, null, null, null).execute().get();
		
			showErrorDialog("Result is: " + result);
		} catch (Exception ee) {
			showErrorDialog("FAIL: " + ee);
		}

	}
	
	private void showErrorDialog(CharSequence msg) {
		new AlertDialog.Builder(this)
			.setTitle(android.R.string.dialog_alert_title)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(msg)
			.setCancelable(true)
			.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).create().show();
	}
	
}
