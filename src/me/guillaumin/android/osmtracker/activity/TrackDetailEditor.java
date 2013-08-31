package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.model.Track;
import me.guillaumin.android.osmtracker.db.model.Track.OSMVisibility;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

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
		for (OSMVisibility v: OSMVisibility.values()) {
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
		
		// Save name field, if changed, to db.
		// String class required for equals to work, and for trim().
		String enteredName = etName.getText().toString().trim();
		if ((enteredName.length() > 0)) {
			values.put(Schema.COL_NAME, enteredName);
		}
		
		// All other values updated even if empty
		values.put(Schema.COL_DESCRIPTION, etDescription.getText().toString().trim());
		values.put(Schema.COL_TAGS, etTags.getText().toString().trim());
		values.put(Schema.COL_OSM_VISIBILITY, OSMVisibility.fromPosition(spVisibility.getSelectedItemPosition()).toString());
		
		getContentResolver().update(trackUri, values, null, null);	

		// All done
		return true;
	}

}
