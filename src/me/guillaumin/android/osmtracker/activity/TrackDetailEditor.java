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
	private EditText etName;

	/** Edit text for track description */
	private EditText etDescription;

	/** Edit text for track tags */
	private EditText etTags;

	/** Spinner for track visibility */
	private Spinner spVisibility;

	
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

	protected void save() {
		// Save changes to db (if any), then finish.
		
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

	}

}
