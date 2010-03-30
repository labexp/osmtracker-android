package me.guillaumin.android.osmtracker.listener;

import java.io.File;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * Manages voice recording.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class VoiceRecOnClickListener implements OnClickListener, OnInfoListener {

	private final static String TAG = VoiceRecOnClickListener.class.getSimpleName();

	/**
	 * Indicates if we are currently recording, to prevent double click.
	 */
	private boolean isRecording = false;

	/**
	 * Dialog shown while recording
	 */
	private ProgressDialog progressDialog;

	/**
	 * Parent activity
	 */
	private TrackLogger activity;
	
	/**
	 * AudioManager, to unmute microphone
	 */
	AudioManager audioManager;

	public VoiceRecOnClickListener(TrackLogger a) {
		activity = a;

		// Try to un-mute microphone, just in case
		audioManager = (AudioManager) a.getSystemService(Context.AUDIO_SERVICE);
	}

	@Override
	public void onClick(View v) {

		if (!isRecording) {

			isRecording = true;

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
			String duration = prefs.getString(OSMTracker.Preferences.KEY_VOICEREC_DURATION,
					OSMTracker.Preferences.VAL_VOICEREC_DURATION);

			// Get a new audio filename
			File audioFile = activity.getGpsLogger().getDataHelper().getNewAudioFile();

			// Show a progress dialog while recording
			progressDialog = new ProgressDialog(v.getContext());
			progressDialog.setTitle(v.getResources().getString(R.string.tracklogger_voicerec_title));
			progressDialog.setMessage(v.getResources().getString(R.string.tracklogger_voicerec_text).replaceAll(
					"\\{0\\}", duration));
			progressDialog.show();

			// Some workaround for record problems
			unMuteMicrophone();
			// The onInfo event is not raised when a GC occurs while recording
			System.gc();
			
			MediaRecorder mediaRecorder = new MediaRecorder();

			try {
				// MediaRecorder configuration

				mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
				mediaRecorder.setMaxDuration(Integer.parseInt(duration) * 1000);
				mediaRecorder.setOnInfoListener(this);

				Log.d(TAG, "Starting voice rec");
				mediaRecorder.prepare();
				mediaRecorder.start();

			} catch (Exception ioe) {
				Log.w("Voice recording has failed", ioe);
				try {
					mediaRecorder.stop();
				} catch (Exception e) {
					Log.w(TAG, "Recording has failed, and MediaPlayer.stop() too");
				} finally {
					mediaRecorder.reset();
					mediaRecorder.release();
				}

				progressDialog.dismiss();
				Toast.makeText(v.getContext(), v.getResources().getString(R.string.error_voicerec_failed),
						Toast.LENGTH_SHORT).show();

				isRecording = false;
			}

			// Still record waypoint, could be usefull even without the voice
			// file.
			Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
			intent.putExtra(OSMTracker.INTENT_KEY_NAME, v.getResources().getString(R.string.wpt_voicerec));
			intent.putExtra(OSMTracker.INTENT_KEY_LINK, audioFile.getName());
			activity.sendBroadcast(intent);
		}
	}

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what) {
			// No need to stop, MediaRecorder has been stopped by system
			mr.reset();
			mr.release();

			// Dismiss dialog
			Log.d(TAG, "Dismissing record dialog");
			
			try {
				progressDialog.dismiss();
			} catch (IllegalArgumentException iae) {
				// View is not attached to window manager. Can occurs
				// if users rotates the phone while recording.

				// TODO This actually leads to Activity leaking the window (see logs
				// while recording & rotating phone. Find a better way to handle
				// that, see View.onDetachedFromWindow();
			}

			isRecording = false;
		}

	}

	/**
	 * Un-mute the microphone, to prevent a blank-recording
	 * on certain devices (Acer Liquid ?)
	 */
	private void unMuteMicrophone() {
		Log.v(TAG, "Unmuting microphone");
		if (audioManager.isMicrophoneMute()) {
			audioManager.setMicrophoneMute(false);
		}
	}
	
}
