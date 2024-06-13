package net.osmtracker.activity;

import android.content.ContentUris;
import android.content.Intent;
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

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;
import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.model.Track;
import net.osmtracker.gpx.ExportToTempFileTask;
import net.osmtracker.osm.OpenStreetMapConstants;
import net.osmtracker.osm.UploadToOpenStreetMapTask;

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
	public final static String OAUTH2_CALLBACK_URL = "osmtracker://osm-upload/oath2-completed/?"+ TrackContentProvider.Schema.COL_TRACK_ID+"=";
	public final static int RC_AUTH = 7;

	private AuthorizationService authService;

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
		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(TrackContentProvider.Schema.COL_TRACK_ID)) {
			return getIntent().getExtras().getLong(TrackContentProvider.Schema.COL_TRACK_ID);
		} else if (getIntent().getData().toString().startsWith(OAUTH2_CALLBACK_URL)) {
			return Long.parseLong(getIntent().getData().getQueryParameter(TrackContentProvider.Schema.COL_TRACK_ID));
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
	}

	/**
	 * Either starts uploading directly if we are authenticated against OpenStreetMap,
	 * or ask the user to authenticate via the browser.
	 */
	private void startUpload() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if ( prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN) ) {
			// Re-use saved token
			uploadToOsm(prefs.getString(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN, ""));
		} else {
			// Open browser and request token
			requestOsmAuth();
		}
	}
	/*
	 * Init Authorization request workflow.
	 */
	public void requestOsmAuth() {
		// Authorization service configuration
		AuthorizationServiceConfiguration serviceConfig =
				new AuthorizationServiceConfiguration(
						Uri.parse(OpenStreetMapConstants.OAuth2.Urls.AUTHORIZATION_ENDPOINT),
						Uri.parse(OpenStreetMapConstants.OAuth2.Urls.TOKEN_ENDPOINT));

		// Obtaining an authorization code
		Uri redirectURI = Uri.parse(OAUTH2_CALLBACK_URL+trackId);
		AuthorizationRequest.Builder authRequestBuilder =
				new AuthorizationRequest.Builder(
						serviceConfig, OpenStreetMapConstants.OAuth2.CLIENT_ID,
						ResponseTypeValues.CODE, redirectURI);
		AuthorizationRequest authRequest = authRequestBuilder
				.setScope(OpenStreetMapConstants.OAuth2.SCOPE)
				.build();

		// Start activity.
		authService = new AuthorizationService(this);
		Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
		startActivityForResult(authIntent, RC_AUTH); //when done onActivityResult will be called.
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// User is returning from authentication
		if (requestCode == RC_AUTH) {
			// Handling the authorization response
			AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
			AuthorizationException ex = AuthorizationException.fromIntent(data);
			// ... process the response or exception ...
			if (ex != null) {
				Log.e(TAG, "Authorization Error. Exception received from server.");
				Log.e(TAG, ex.getMessage());
			} else if (resp == null) {
				Log.e(TAG, "Authorization Error. Null response from server.");
			} else {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

				//Exchanging the authorization code
				authService.performTokenRequest(
						resp.createTokenExchangeRequest(),
						new AuthorizationService.TokenResponseCallback() {
							@Override public void onTokenRequestCompleted(
									TokenResponse resp, AuthorizationException ex) {
								if (resp != null) {
									// exchange succeeded
									SharedPreferences.Editor editor = prefs.edit();
									editor.putString(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN, resp.accessToken);
									editor.apply();
									//continue with the track Upload.
									uploadToOsm(resp.accessToken);
								} else {
									// authorization failed, check ex for more details
									Log.e(TAG, "OAuth failed.");
								}
							}
						});
			}
		} else {
			Log.e(TAG, "Unexpected requestCode:" + requestCode + ".");
		}
	}

	/**
	 * Exports track on disk then upload to OSM.
	 */
	public void uploadToOsm(String accessToken) {
		new ExportToTempFileTask(this, trackId) {
			@Override
			protected void executionCompleted() {
				new UploadToOpenStreetMapTask(OpenStreetMapUpload.this, accessToken,
						trackId, this.getTmpFile(),	this.getFilename(),
						etDescription.getText().toString(), etTags.getText().toString(),
						Track.OSMVisibility.fromPosition(
								OpenStreetMapUpload.this.spVisibility.getSelectedItemPosition())
				).execute();
			}
		}.execute();
	}

}
