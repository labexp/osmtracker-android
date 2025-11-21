package net.osmtracker.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;
import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.osm.OpenStreetMapConstants;
import net.osmtracker.osm.UploadToOpenStreetMapNotesTask;

/**
 * <p>Uploads a note on OSM using the API and
 * OAuth authentication.</p>
 *
 * <p>This activity may be called twice during a single
 * upload cycle: First to start the upload, then a second
 * time when the user has authenticated using the browser.</p>
 *
 * @author Most of the code was made by Nicolas Guillaumin, adapted by Jose Andrés Vargas Serrano
 */
public class OpenStreetMapNotesUpload extends Activity {

    private static final String TAG = OpenStreetMapNotesUpload.class.getSimpleName();

    private double latitude;
    private double longitude;

    private TextView noteContentView;
    private TextView noteFooterView;

    /** URL that the browser will call once the user is authenticated */
    public final static String OAUTH2_CALLBACK_URL = "osmtracker://osm-upload/oath2-completed/";
    public final static int RC_AUTH = 7;

    private AuthorizationService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View uploadNoteView = getLayoutInflater().inflate(R.layout.osm_note_upload, null);
        setContentView(uploadNoteView);
        setTitle(R.string.osm_note_upload);

        noteContentView = uploadNoteView.findViewById(R.id.wplist_item_name);
        noteFooterView = uploadNoteView.findViewById(R.id.osm_note_footer);

        // Read and cache extras
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e(TAG, "Missing extras for note upload.");
            finish();
            return;
        }

        String initialNoteText = extras.getString("noteContent", "");
        String appName = extras.getString("appName", getString(R.string.app_name));
        String version = extras.getString("version", "");

        if (extras.containsKey("latitude")) latitude = extras.getDouble("latitude");
        if (extras.containsKey("longitude")) longitude = extras.getDouble("longitude");

        // fill UI with note content and note footer
        noteContentView.setText(initialNoteText);
        noteFooterView.setText(getString(R.string.osm_note_footer, appName, version));

        final Button btnOk = (Button) findViewById(R.id.osm_note_upload_button_ok);
        btnOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpload();
            }
        });
        final Button btnCancel = (Button) findViewById(R.id.osm_note_upload_button_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

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
        Uri redirectURI = Uri.parse(OAUTH2_CALLBACK_URL);
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
                                //continue with the note Upload.
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
     * Uploads notes to OSM.
     */
    public void uploadToOsm(String accessToken) {
        String noteText = noteContentView.getText().toString();
        String footer = noteFooterView.getText().toString();
        if (!footer.isEmpty()) {
            noteText = noteText + "\n\n" + footer;
        }
        new UploadToOpenStreetMapNotesTask(
                OpenStreetMapNotesUpload.this,
                accessToken,
                noteText,
                latitude,
                longitude
        ).execute();
    }


}
