package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.model.Track;
import me.guillaumin.android.osmtracker.db.model.Track.OSMVisibility;
import me.guillaumin.android.osmtracker.gpx.ExportToTempFileTask;
import me.guillaumin.android.osmtracker.osm.OpenStreetMapConstants;
import me.guillaumin.android.osmtracker.osm.RetrieveAccessTokenTask;
import me.guillaumin.android.osmtracker.osm.RetrieveRequestTokenTask;
import me.guillaumin.android.osmtracker.osm.UploadToOpenStreetMapTask;
import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import android.content.ContentUris;
import android.content.SharedPreferences;
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
 * <p>Uploads a track on OSM using the API and
 * OAuth authentication.</p>
 * 
 * <p>This activity may be called twice during a single
 * upload cycle: First to start the upload, then a second
 * time when the user has authenticated using the browser.</p>
 *
 * @author Nicolas Guillaumin
 */
public class OpenStreetMapUpload extends TrackDetailEditor {

	private static final String TAG = OpenStreetMapUpload.class.getSimpleName();

	/** URL that the browser will call once the user is authenticated */
	private static final String OAUTH_CALLBACK_URL = "osmtracker://osm-upload/oath-completed/?"+Schema.COL_TRACK_ID+"=";
	
	private static final CommonsHttpOAuthProvider oAuthProvider = new CommonsHttpOAuthProvider(
			OpenStreetMapConstants.OAuth.Urls.REQUEST_TOKEN_URL,
			OpenStreetMapConstants.OAuth.Urls.ACCESS_TOKEN_URL,
			OpenStreetMapConstants.OAuth.Urls.AUTHORIZE_TOKEN_URL);
	private static final CommonsHttpOAuthConsumer oAuthConsumer = new CommonsHttpOAuthConsumer(
			OpenStreetMapConstants.OAuth.CONSUMER_KEY,
			OpenStreetMapConstants.OAuth.CONSUMER_SECRET);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState, R.layout.osm_upload, getTrackId());
		fieldsMandatory = true;

		final Button btnOk = (Button) findViewById(R.id.osm_upload_btn_ok);
		btnOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (save()) {
					startUpload();
				}
			}
		});
				
		final Button btnCancel = (Button) findViewById(R.id.osm_upload_btn_cancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
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
		} else if (getIntent().getData().toString().startsWith(OAUTH_CALLBACK_URL)) {
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
		if (uri != null && uri.toString().startsWith(OAUTH_CALLBACK_URL)) {
			// User is returning from authentication
			String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
			
			new RetrieveAccessTokenTask(this, oAuthProvider, oAuthConsumer, verifier).execute();
		}
	}

	/**
	 * Either starts uploading directly if we are authenticated against OpenStreetMap,
	 * or ask the user to authenticate via the browser.
	 */
	private void startUpload() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if ( prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN)
				&& prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET)) {
			// Re-use saved token
			oAuthConsumer.setTokenWithSecret(
					prefs.getString(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN, ""),
					prefs.getString(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET, ""));
			uploadToOsm();
		} else {
			// Open browser and request token
			new RetrieveRequestTokenTask(this, oAuthProvider, oAuthConsumer, OAUTH_CALLBACK_URL+trackId).execute();
		}
	}

	/**
	 * Exports track on disk then upload to OSM.
	 */
	public void uploadToOsm() {
		new ExportToTempFileTask(this, trackId) {
			@Override
			protected void executionCompleted() {
				new UploadToOpenStreetMapTask(OpenStreetMapUpload.this, trackId, oAuthConsumer, this.getTmpFile(),
						this.getFilename(), etDescription.getText().toString(), etTags.getText().toString(),
						OSMVisibility.fromPosition(OpenStreetMapUpload.this.spVisibility.getSelectedItemPosition()))
							.execute();
			}
		}.execute();
	}

}
