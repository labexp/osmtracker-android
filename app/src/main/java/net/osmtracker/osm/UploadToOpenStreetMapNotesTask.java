package net.osmtracker.osm;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.util.DialogUtils;

import java.lang.ref.WeakReference;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.common.errors.OsmAuthorizationException;
import de.westnordost.osmapi.common.errors.OsmBadUserInputException;
import de.westnordost.osmapi.map.data.OsmLatLon;
import de.westnordost.osmapi.notes.NotesApi;

/**
 * Uploads a note to OpenStreetMap
 *
 * @author Most of the code was made by Nicolas Guillaumin, adapted by Jose Andrés Vargas Serrano
 */
public class UploadToOpenStreetMapNotesTask {

    private static final String TAG = UploadToOpenStreetMapNotesTask.class.getSimpleName();

	// Result constants
	private static final int RESULT_OK = 1;
	private static final int RESULT_ERROR_INTERNAL = -1;
	private static final int RESULT_ERROR_AUTH = -2;
	private static final int RESULT_ERROR_OSM_USER = -3;

	private final WeakReference<Activity> activityRef;
    private final String accessToken;

	//OSM Note data
	private final long noteId;
    private final String noteText;
    private final double longitude;
    private final double latitude;

	private AlertDialog progressDialog;
    // Error message returned by OSM if the request completed
    private String errorMsg;
	private int resultCode = RESULT_ERROR_INTERNAL;

	public UploadToOpenStreetMapNotesTask(Activity activity, String accessToken, long noteId,
										  String noteText, double latitude, double longitude) {
		this.activityRef = new WeakReference<>(activity);
		this.accessToken = accessToken;
		this.noteId = noteId;
		this.noteText = noteText;
		this.longitude = longitude;
		this.latitude = latitude;
	}

	/**
	 * Synchronous execution for use with ExecutorService.
	 * Replaces the lifecycle of AsyncTask. */
	public void run() {
		final Activity activity = activityRef.get();
		if (activity == null || activity.isFinishing()) return;

		// 1. Prepare UI (Equivalent to onPreExecute)
		activity.runOnUiThread(() -> progressDialog = createProgressDialog(activity));

		// 2. Execute Network Logic (Equivalent to doInBackground)
		OsmConnection osm = new OsmConnection(
				OpenStreetMapConstants.Api.OSM_API_URL_PATH,
				OpenStreetMapConstants.OAuth2.USER_AGENT,
				accessToken);

		try {
			new NotesApi(osm).create(new OsmLatLon(latitude, longitude), noteText);
			resultCode = RESULT_OK;
		} catch (OsmBadUserInputException e) {
			Log.e(TAG, "Bad OSM user input or illegal argument", e);
			errorMsg = e.getLocalizedMessage();
			resultCode = RESULT_ERROR_OSM_USER;
		} catch (OsmAuthorizationException oae) {
			Log.e(TAG, "OSM Authorization error", oae);
			errorMsg = oae.getLocalizedMessage();
			resultCode = RESULT_ERROR_AUTH;
		} catch (Exception e) {
			Log.e(TAG, "Upload error", e);
			errorMsg = e.getLocalizedMessage();
			resultCode = RESULT_ERROR_INTERNAL;
		}

		// 3. Handle Results (Equivalent to onPostExecute)
		activity.runOnUiThread(() -> {
			Activity act = activityRef.get();
			if (act == null || act.isFinishing()) return;

			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}

			handleResult(act);
		});
	}

	private void handleResult(Activity activity) {
		switch (resultCode) {
			case RESULT_OK:
				DataHelper.setNoteUploadDate(noteId,
						System.currentTimeMillis(), activity.getContentResolver());
				new AlertDialog.Builder(activity)
						.setIcon(android.R.drawable.ic_dialog_info)
						.setMessage(R.string.osm_upload_sucess)
						.setPositiveButton(android.R.string.ok,
								(d, w) -> activity.finish())
						.show();
				break;

			case RESULT_ERROR_AUTH:
				showAuthErrorDialog(activity);
				break;

			default:
				DialogUtils.showErrorDialog(activity,
						activity.getString(R.string.osm_note_upload_error) + ": " + errorMsg);
				break;
		}
	}

	private AlertDialog createProgressDialog(Activity activity) {
		LinearLayout layout = new LinearLayout(activity);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setPadding(50, 50, 50, 50);
		layout.setGravity(Gravity.CENTER_VERTICAL);

		ProgressBar pb = new ProgressBar(activity);
		pb.setIndeterminate(true);
		layout.addView(pb);

		TextView tv = new TextView(activity);
		tv.setText(R.string.osm_note_upload);
		tv.setPadding(40, 0, 0, 0);
		layout.addView(tv);

		AlertDialog progressDialog = new AlertDialog.Builder(activity)
				.setView(layout).setCancelable(false).create();
		progressDialog.show();
		return progressDialog;
	}

	/**
	 * Helper to show the specific authorization error dialog
	 */
	private void showAuthErrorDialog(Activity activity) {
		new AlertDialog.Builder(activity)
				.setTitle(android.R.string.dialog_alert_title)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.osm_note_upload_unauthorized)
				.setCancelable(true)
				.setNegativeButton(android.R.string.no, (d, w) -> d.dismiss())
				.setPositiveButton(android.R.string.yes, (d, w) -> {
					PreferenceManager.getDefaultSharedPreferences(activity).edit()
						.remove(OSMTracker.Preferences.KEY_OSM_OAUTH2_ACCESSTOKEN).apply();
					d.dismiss();
				}).show();
	}

}
