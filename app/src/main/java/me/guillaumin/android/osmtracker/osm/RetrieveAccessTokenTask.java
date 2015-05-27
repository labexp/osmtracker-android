package me.guillaumin.android.osmtracker.osm;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.OpenStreetMapUpload;
import me.guillaumin.android.osmtracker.util.DialogUtils;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * <p>Retrieves the OAuth access token and starts the upload.</p>
 * 
 * <p>Cannot be done directly in the upload activity as network is not
 * permitted on the UI thread.</p>
 * 
 */
public class RetrieveAccessTokenTask extends AsyncTask<Void, Void, Void> {

	private static final String TAG = RetrieveAccessTokenTask.class.getSimpleName();
	
	private final OpenStreetMapUpload activity;
	private final CommonsHttpOAuthProvider oAuthProvider;
	private final CommonsHttpOAuthConsumer oAuthConsumer;
	private final String verifier;

	private OAuthException oAuthException = null;
	
	public RetrieveAccessTokenTask(OpenStreetMapUpload activity,
			CommonsHttpOAuthProvider oAuthProvider,
			CommonsHttpOAuthConsumer oAuthConsumer,
			String verifier) {
		this.activity = activity;
		this.oAuthProvider = oAuthProvider;
		this.oAuthConsumer = oAuthConsumer;
		this.verifier = verifier;
	}
	
	@Override
	protected Void doInBackground(Void... params) {

		try {
			oAuthProvider.retrieveAccessToken(oAuthConsumer, verifier);
			
			// Store token in preferences for future use
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
			Editor editor = prefs.edit();
			editor.putString(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN, oAuthConsumer.getToken());
			editor.putString(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET, oAuthConsumer.getTokenSecret());
			editor.commit();
		} catch (OAuthException oe) {
			Log.e(TAG, "Could not retrieve access token", oe);
			oAuthException = oe;
			cancel(false);			
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		DialogUtils.showErrorDialog(activity, activity.getResources().getString(R.string.osm_upload_oauth_failed) + ": " + oAuthException);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		activity.uploadToOsm();
	}

}
