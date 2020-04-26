package net.osmtracker.activity;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import net.osmtracker.R;
import net.osmtracker.db.DataHelper;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.model.Track;

import java.util.Date;

/**
 * Base class for activities that edit track details.
 *
 */
public abstract class TrackDetailEditor extends Activity {

	/** Current track ID */
	protected long trackId;

	/** Edit text for the track name */
	protected EditText etName;

	/** Edit text for track description */
	protected EditText etDescription;

	/** Edit text for track tags */
	protected EditText etTags;

	/** Spinner for track visibility */
	protected Spinner spVisibility;
	
	/** Whereas to verify if mandatory fields are filled or not */
	protected boolean fieldsMandatory = false;
	
	protected void onCreate(Bundle savedInstanceState, int viewResId, long trackId) {
		super.onCreate(savedInstanceState);
		
		this.trackId = trackId;
		
		setContentView(viewResId);
		setTitle(getTitle() + ": #" + trackId);
		
		etName = (EditText) findViewById(R.id.trackdetail_item_name);
		etDescription = (EditText) findViewById(R.id.trackdetail_item_description);
		etTags = (EditText) findViewById(R.id.trackdetail_item_tags);
		spVisibility = (Spinner) findViewById(R.id.trackdetail_item_osm_visibility);

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item);
		for (Track.OSMVisibility v: Track.OSMVisibility.values()) {
			adapter.add(getResources().getString(v.resId));
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spVisibility.setAdapter(adapter);

	}
	
	protected void bindTrack(Track t) {
		if (etName.length() == 0) {
			etName.setText(t.getName());
		}

		etDescription.setText(t.getDescription());
		etTags.setText(t.getCommaSeparatedTags());
		spVisibility.setSelection(t.getVisibility().position);
	}

	/**
	 * Saves the new information in database
	 * @return false if the save didn't take place, true otherwise.
	 */
	protected boolean save() {
		// Save changes to db (if any), then finish.
		etDescription.setError(null);
		if (fieldsMandatory) {
			if (etDescription.getText().length() < 1) {
				etDescription.setError(getResources().getString(R.string.trackdetail_description_mandatory));
				return false;
			}
		}
		
		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		ContentValues values = new ContentValues();

		Cursor cursor = getContentResolver().query(trackUri, null, null,
				null, null);

		long startDateLong = 0;
		String tname = "";
		if (cursor != null && cursor.moveToFirst()) {
			startDateLong = cursor.getLong(cursor.getColumnIndex(TrackContentProvider.Schema.COL_START_DATE));
			tname = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
			cursor.close();
		}

		// Saved track startDate
		Date startDate = new Date(startDateLong);

		// Save name field, if changed, to db.
		// String class required for equals to work, and for trim().
		String enteredName = etName.getText().toString().trim();

		// Get default track name
        String defaultDate = DataHelper.FILENAME_FORMATTER.format(startDate);

        // If no changes were made don't append the date
		if ((enteredName.length() > 0 && !enteredName.equals(tname))) {
			// Append date to avoid duplicated tracks names only if is not included already
			if(!enteredName.contains(defaultDate)) enteredName += "_" + defaultDate;
			values.put(TrackContentProvider.Schema.COL_NAME, enteredName);
		}
		
		// All other values updated even if empty
		values.put(TrackContentProvider.Schema.COL_DESCRIPTION, etDescription.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_TAGS, etTags.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_OSM_VISIBILITY, Track.OSMVisibility.fromPosition(spVisibility.getSelectedItemPosition()).toString());
		
		getContentResolver().update(trackUri, values, null, null);	

		// All done
		return true;
	}

}
