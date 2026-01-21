package net.osmtracker.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.osm.OpenStreetMapConstants;
import net.osmtracker.osm.UploadToOpenStreetMapNotesTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>Uploads a note on OSM using the API and OAuth authentication.</p>
 *
 * <p>This activity may be called twice during a single
 * upload cycle: First to start the upload, then a second
 * time when the user has authenticated using the browser.</p>
 *
 * @author Most of the code was made by Nicolas Guillaumin, adapted by Jose Andrés Vargas Serrano
 */
public class OpenStreetMapNotesUpload extends AppCompatActivity {

    private static final String TAG = OpenStreetMapNotesUpload.class.getSimpleName();

	private long noteId;

    private double latitude;
    private double longitude;

    private TextView noteContentView;
    private TextView noteFooterView;

    /** URL that the browser will call once the user is authenticated */
    public final static String OAUTH2_CALLBACK_URL = "osmtracker://osm-upload/oath2-completed/";
    private AuthorizationService authService;
	private ActivityResultLauncher<Intent> authLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Register the launcher
		authLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					// This replaces the logic previously in onActivityResult
					Intent data = result.getData();
					// RC_AUTH logic
					if (data != null) {
						AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
						AuthorizationException ex = AuthorizationException.fromIntent(data);

						if (resp != null) {
							exchangeAuthorizationCode(resp);
						} else {
							Log.e(TAG, "Authorization failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
							Toast.makeText(this, R.string.osm_upload_oauth_failed, Toast.LENGTH_SHORT).show();
						}
					}
				}
		);


		setContentView(R.layout.osm_note_upload);
		setTitle(R.string.osm_note_upload);
		noteContentView = findViewById(R.id.wplist_item_name);
		noteFooterView = findViewById(R.id.osm_note_footer);

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

		if (extras.containsKey("noteId")) noteId = extras.getLong("noteId");
        if (extras.containsKey("latitude")) latitude = extras.getDouble("latitude");
        if (extras.containsKey("longitude")) longitude = extras.getDouble("longitude");

        // fill UI with note content and note footer
        noteContentView.setText(initialNoteText);
        noteFooterView.setText(getString(R.string.osm_note_footer, appName, version));

        final Button btnOk = findViewById(R.id.osm_note_upload_button_ok);
        btnOk.setOnClickListener(v -> startUpload(noteId));
        final Button btnCancel = findViewById(R.id.osm_note_upload_button_cancel);
        btnCancel.setOnClickListener(v -> finish());

    }


    /**
     * Either starts uploading directly if we are authenticated against OpenStreetMap,
     * or ask the user to authenticate via the browser.
     */
    private void startUpload(long noteId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String accessToken = prefs.getString(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN, null);

		if (accessToken != null && !accessToken.isEmpty()) {
			// STATE: AUTHORIZED. Re-use saved token
			Log.d(TAG, "Token found, proceeding to upload note to OSM.");
			uploadToOsm(accessToken, noteId);
		} else {
			// STATE: UNAUTHORIZED. Open browser and request token
			Log.d(TAG, "No token found, requesting authorization.");
            requestOsmAuth();
        }
    }
    /*
     * Init Authorization request workflow. Launches browser to request authorization.
     */
    public void requestOsmAuth() {
        // Authorization service configuration
        AuthorizationServiceConfiguration serviceConfig = new AuthorizationServiceConfiguration(
                Uri.parse(OpenStreetMapConstants.OAuth2.Urls.AUTHORIZATION_ENDPOINT),
                Uri.parse(OpenStreetMapConstants.OAuth2.Urls.TOKEN_ENDPOINT));

		// Obtaining an authorization code
		AuthorizationRequest authRequest = new AuthorizationRequest.Builder(
				serviceConfig,
				OpenStreetMapConstants.OAuth2.CLIENT_ID,
				ResponseTypeValues.CODE,
				Uri.parse(OAUTH2_CALLBACK_URL))
				.setScope(OpenStreetMapConstants.OAuth2.SCOPE)
				.build();

		// Start activity.
        authService = new AuthorizationService(this);
        Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
		//when done onActivityResult will be called.
		// Use the launcher instead of startActivityForResult
		authLauncher.launch(authIntent);
    }

	private void exchangeAuthorizationCode(AuthorizationResponse resp) {
		authService.performTokenRequest(resp.createTokenExchangeRequest(), (tokenResp, tokenEx) -> {
			if (tokenResp != null && tokenResp.accessToken != null) {
				// STATE: TRANSITION TO AUTHORIZED
				persistToken(tokenResp.accessToken);
				uploadToOsm(tokenResp.accessToken, noteId);
			} else {
				Log.e(TAG, "Token exchange failed");
			}
		});
	}

	private void persistToken(String token) {
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putString(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN, token)
				.apply();
	}

    /**
     * Uploads notes to OSM.
     */
    public void uploadToOsm(String accessToken, long noteId) {
		String noteText = noteContentView.getText().toString();
		String footer = noteFooterView.getText().toString();
		if (!footer.isEmpty()) {
			noteText = noteText + "\n\n" + footer;
		}

		// Final variables for the background thread
		final String finalNoteText = noteText;

		// This replaces the deprecated AsyncTask.execute()
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(() -> {
			try {
				new UploadToOpenStreetMapNotesTask(
						OpenStreetMapNotesUpload.this,
						accessToken,
						noteId,
						finalNoteText,
						latitude,
						longitude
				).run();
			} catch (Exception e) {
				Log.e(TAG, "Error during OSM Note upload", e);
				runOnUiThread(() ->
						Toast.makeText(this, R.string.osm_upload_error, Toast.LENGTH_SHORT).show()
				);
			} finally {
				executor.shutdown();
			}
		});
	}

}
