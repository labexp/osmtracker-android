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
import net.osmtracker.util.DialogUtils;

import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.common.errors.OsmAuthorizationException;
import de.westnordost.osmapi.common.errors.OsmBadUserInputException;
import de.westnordost.osmapi.map.data.OsmLatLon;
import de.westnordost.osmapi.notes.Note; // Note object
import de.westnordost.osmapi.notes.NotesApi; // Api for uploading notes to OSM
import de.westnordost.osmapi.map.data.LatLon; // Data type for location points, maybe I'll put it in the dialog file

/**
 * Uploads a note to OpenStreetMap
 *
 * @author Most of the code was made by Nicolas Guillaumin, adapted by Jose Andrés Vargas Serrano
 */
public class UploadToOpenStreetMapNotesTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = UploadToOpenStreetMapNotesTask.class.getSimpleName();

    /** Upload progress dialog */
    private ProgressDialog dialog;

    private final Activity activity;
    private final String accessToken;

	private final long noteId;

    /** Note text */
    private final String noteText;

    /** Note longitude */
    private final double longitude;

    /** Note latitude */
    private final double latitude;

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

    // Not using an activity yet
    public UploadToOpenStreetMapNotesTask(Activity activity, String accessToken, long noteId,
										  String noteText, double latitude, double longitude) {
        this.activity = activity;
        this.accessToken = accessToken;
		this.noteId = noteId;
        this.noteText = noteText;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    @Override
    protected void onPreExecute() {
        try {
            // Display progress dialog
            dialog = new ProgressDialog(activity);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(true);
            dialog.setTitle(R.string.osm_note_upload);

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
                        activity.getResources().getString(R.string.osm_note_upload_error)
                                + ": " + errorMsg);
                break;
            case okResultCode:
                dialog.dismiss();
				// Success ! Update database and close activity
				DataHelper.setNoteUploadDate(noteId, System.currentTimeMillis(), activity.getContentResolver());

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
                Log.e(TAG, "onPostExecute() authorization failed: " + errorMsg + " (" + resultCode + ")");
                // Authorization issue. Provide a way to clear credentials
                new AlertDialog.Builder(activity)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.osm_note_upload_unauthorized)
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
                break;

            default:
                // Another error. Display OSM response
                dialog.dismiss();
                // Internal error, the request didn't start at all
                Log.e(TAG, "onPostExecute() default failed: " + errorMsg + " (" + resultCode + ")");
                DialogUtils.showErrorDialog(activity,
                        activity.getResources().getString(R.string.osm_note_upload_error)
                                + ": " + errorMsg);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        OsmConnection osm = new OsmConnection(OpenStreetMapConstants.Api.OSM_API_URL_PATH,
            OpenStreetMapConstants.OAuth2.USER_AGENT, accessToken);

        try {
            LatLon point = new OsmLatLon(latitude, longitude);
            Note note = new NotesApi(osm).create(point, noteText);
            resultCode = okResultCode;
        } catch (/*IOException |*/  IllegalArgumentException | OsmBadUserInputException e) {
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

}
