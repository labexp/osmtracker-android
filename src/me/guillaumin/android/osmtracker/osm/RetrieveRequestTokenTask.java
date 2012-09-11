package me.guillaumin.android.osmtracker.osm;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.util.DialogUtils;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/**
 * <p>Retrieves the OAuth request token (opens the browser
 * for the user to log in if necessary).</p>
 * 
 * <p>Cannot be done directly in the upload activity as network is not
 * permitted on the UI thread.</p>
 */
public class RetrieveRequestTokenTask extends AsyncTask<Void, Void, Void> {

	private static final String TAG = RetrieveRequestTokenTask.class.getSimpleName();
	
	private final Context context;
	private final CommonsHttpOAuthProvider oAuthProvider;
	private final CommonsHttpOAuthConsumer oAuthConsumer;
	private final String callbackUrl;

	private OAuthException oAuthException = null;
	private String requestTokenUrl;
	
	public RetrieveRequestTokenTask(Context context,
			CommonsHttpOAuthProvider oAuthProvider,
			CommonsHttpOAuthConsumer oAuthConsumer, String callbackUrl) {
		this.context = context;
		this.oAuthProvider = oAuthProvider;
		this.oAuthConsumer = oAuthConsumer;
		this.callbackUrl = callbackUrl;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		try {
			requestTokenUrl = oAuthProvider.retrieveRequestToken(oAuthConsumer, callbackUrl);
		} catch (OAuthException oe) {
			Log.e(TAG, "Could not retrieve request token", oe);
			oAuthException = oe;
			cancel(false);			
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		DialogUtils.showErrorDialog(context,
				context.getResources().getString(R.string.osm_upload_oauth_failed) + ": " + oAuthException);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestTokenUrl)));
	}

}
