package net.osmtracker.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.activity.TrackListRVAdapter.TrackItemVH;

/**
 * Lists existing tracks. Each track is displayed using {@link RecyclerView}
 *
 *  @author Alain Knaff
 */
public class TrackPicker extends AppCompatActivity
	implements TrackListRVAdapter.TrackListRecyclerViewAdapterListener
{
	private static final String TAG = TrackPicker.class.getSimpleName();

	/** Constant used if no track is active (-1)*/
	private static final long TRACK_ID_NO_TRACK = -1;

	// The active track being recorded, if any, or {TRACK_ID_NO_TRACK};
	// value is updated in {@link #onResume()}
	private long currentTrackId = TRACK_ID_NO_TRACK;

	private TrackListRVAdapter recyclerViewAdapter;

	private RecyclerView recyclerView;

	
	// To check if the RecyclerView already has a
	// DividerItemDecoration added
	private boolean hasDivider;

	private long overlaidTrackId;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trackpicker);

		// Toolbar myToolbar = findViewById(R.id.my_toolbar);
		// setSupportActionBar(myToolbar);

		recyclerView = findViewById(R.id.recyclerview);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		// Adding a horizontal divider
		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
		dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider)); // Using a custom drawable

		recyclerView.addItemDecoration(dividerItemDecoration);
	}

	@Override
	protected void onResume() {
		activatedItem = null;
		Intent i = getIntent();
		overlaidTrackId = i.getLongExtra(OSMTracker.INTENT_KEY_OVERLAID_TRACK_ID, 0);
		currentTrackId = i.getLongExtra(TrackContentProvider.Schema.COL_TRACK_ID, 0);
		setRecyclerView();
		super.onResume();
	}


	/**
	 * Configures and initializes the RecyclerView for displaying the list of tracks.
	 */
	private void setRecyclerView() {
		RecyclerView recyclerView = findViewById(R.id.recyclerview);

		LinearLayoutManager layoutManager = new LinearLayoutManager(this,
				LinearLayoutManager.VERTICAL, false);
		recyclerView.setLayoutManager(layoutManager);
		// adds a divider decoration if not already present
		if (!hasDivider) {
			DividerItemDecoration did = new DividerItemDecoration(recyclerView.getContext(),
					layoutManager.getOrientation());
			recyclerView.addItemDecoration(did);
			hasDivider = true;
		}
		recyclerView.setHasFixedSize(true);
		Cursor cursor = getContentResolver().query(
				TrackContentProvider.CONTENT_URI_TRACK, null, null, null,
				TrackContentProvider.Schema.COL_START_DATE + " desc");

		recyclerViewAdapter = new TrackListRVAdapter(this, cursor, this);
		recyclerView.setAdapter(recyclerViewAdapter);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo, long trackId) {
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	private TrackItemVH activatedItem = null;
	
	@Override
	public void onClick(TrackItemVH item, long trackId) {
		Intent i;

		if(trackId == this.currentTrackId)
			// ignore clicks on current track
			return;

		overlaidTrackId = select(item, trackId);
		Intent data = new Intent();
		data.putExtra(OSMTracker.INTENT_KEY_OVERLAID_TRACK_ID,
			      overlaidTrackId);
		setResult(RESULT_OK, data);
		finish();
	}

	private long select(TrackItemVH item, long trackId) {
		if(activatedItem != null)
			// if a track was activated, whether same or another,
			// deactivate
			activatedItem.activate(false);

		if(trackId == this.overlaidTrackId)
			// if previously activated track was clicked, this
			// means user doesn't want any track to be activated
			return 0;

		item.activate(true);
		return trackId;
	}

	@Override
	public void initializeItem(TrackItemVH item, long trackId) {
		if(trackId == this.currentTrackId)
			// prevent user from clicking on same track
			item.itemView.setEnabled(false);
		if(trackId == this.overlaidTrackId) {
			item.activate(true);
			activatedItem = item;
		}
	}
}
