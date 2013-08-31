package me.guillaumin.android.osmtracker.activity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.guillaumin.android.osmtracker.OSMTracker;
import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.overlay.WayPointsOverlay;

import org.osmdroid.contributor.util.constants.OpenStreetMapContributorConstants;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.SimpleLocationOverlay;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Display current track over an OSM map.
 * Based on osmdroid code http://osmdroid.googlecode.com/
 *<P>
 * Used only if {@link OSMTracker.Preferences#KEY_UI_DISPLAYTRACK_OSM} is set.
 * Otherwise {@link DisplayTrack} is used (track only, no OSM background tiles).
 * 
 * @author Viesturs Zarins
 *
 */
public class DisplayTrackMap extends Activity implements OpenStreetMapContributorConstants{

	private static final String TAG = DisplayTrackMap.class.getSimpleName();
	
	/**
	 * Key for keeping the zoom level in the saved instance bundle
	 */
	private static final String CURRENT_ZOOM = "currentZoom";

	/**
	 *  Key for keeping scrolled left position of OSM view activity re-creation
	 * 
	 */
	private static final String CURRENT_SCROLL_X = "currentScrollX";

	/** 
	 * Key for keeping scrolled top position of OSM view across activity re-creation
	 * 
	 */ 
	private static final String CURRENT_SCROLL_Y = "currentScrollY";

	/**
	 *  Key for keeping whether the map display should be centered to the gps location 
	 * 
	 */
	private static final String CURRENT_CENTER_TO_GPS_POS = "currentCenterToGpsPos";

	/**
	 *  Key for keeping whether the map display was zoomed and centered
	 *  on an old track id loaded from the database (boolean {@link #zoomedToTrackAlready})
	 */
	private static final String CURRENT_ZOOMED_TO_TRACK = "currentZoomedToTrack";

	/**
	 * Key for keeping the last zoom level across app. restart
	 */
	private static final String LAST_ZOOM = "lastZoomLevel";

	/**
	 * Default zoom level
	 */
	private static final int DEFAULT_ZOOM  = 16;

	/**
	 * Main OSM view
	 */
	private MapView osmView;
	
	/**
	 * Controller to interact with view
	 */
	private MapController osmViewController;
	
	/**
	 * OSM view overlay that displays current location
	 */
	private SimpleLocationOverlay myLocationOverlay;
	
	/**
	 * OSM view overlay that displays current path
	 */
	private PathOverlay pathOverlay;

	/**
	 * OSM view overlay that displays waypoints 
	 */
	private WayPointsOverlay wayPointsOverlay;	
	
	/**
	 * Current track id
	 */
	private long currentTrackId;
	
	/**
	 * whether the map display should be centered to the gps location 
	 */
	private boolean centerToGpsPos = true;
	
	/**
	 * whether the map display was already zoomed and centered
	 * on an old track loaded from the database (should be done only once).
	 */
	private boolean zoomedToTrackAlready = false;
	
	/**
	 * the last position we know
	 */
	private GeoPoint currentPosition;

	/**
	 * The row id of the last location read from the database that has been added to the
	 * list of layout points. Using this we to reduce DB load by only reading new points.
	 * Initially null, to indicate that no data has yet been read.  
	 */
	private Integer lastTrackPointIdProcessed = null;
	
	/**
	 * Observes changes on trackpoints
	 */
	private ContentObserver trackpointContentObserver;

	/**
	 * Keeps the SharedPreferences
	 */
	private SharedPreferences prefs = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// loading the preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		setContentView(R.layout.displaytrackmap);
		
		currentTrackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		setTitle(getTitle() + ": #" + currentTrackId);
		
		// Initialize OSM view
		osmView = (MapView) findViewById(R.id.displaytrackmap_osmView);
		osmView.setMultiTouchControls(true);  // pinch to zoom
		// we'll use osmView to define if the screen is always on or not
		osmView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
		osmViewController = osmView.getController();
		
		// Check if there is a saved zoom level
		if(savedInstanceState != null) {
			osmViewController.setZoom(savedInstanceState.getInt(CURRENT_ZOOM, DEFAULT_ZOOM));
			osmView.scrollTo(savedInstanceState.getInt(CURRENT_SCROLL_X, 0),
					savedInstanceState.getInt(CURRENT_SCROLL_Y, 0));
			centerToGpsPos = savedInstanceState.getBoolean(CURRENT_CENTER_TO_GPS_POS, centerToGpsPos);
			zoomedToTrackAlready = savedInstanceState.getBoolean(CURRENT_ZOOMED_TO_TRACK, zoomedToTrackAlready);
		} else {
			// Try to get last zoom Level from Shared Preferences
			SharedPreferences settings = getPreferences(MODE_PRIVATE);
			osmViewController.setZoom(settings.getInt(LAST_ZOOM, DEFAULT_ZOOM));
		}
		
		selectTileSource();

		createOverlays();

		// Create content observer for trackpoints
		trackpointContentObserver = new ContentObserver(new Handler()) {
			@Override
			public void onChange(boolean selfChange) {
				pathChanged();
			}
		};
		
		// Register listeners for zoom buttons
		findViewById(R.id.displaytrackmap_imgZoomIn).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				osmViewController.zoomIn();
			}
		});
		findViewById(R.id.displaytrackmap_imgZoomOut).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				osmViewController.zoomOut();
			}
		});
	}

	/**
	 * Sets the map tile provider according to the user's demands in the settings.
	 */
	public void selectTileSource() {
		String mapTile = prefs.getString(OSMTracker.Preferences.KEY_UI_MAP_TILE, OSMTracker.Preferences.VAL_UI_MAP_TILE_MAPNIK);
		osmView.setTileSource(selectMapTile(mapTile));
	}
	
	/**
	 * Returns a ITileSource for the map according to the selected mapTile
	 * String. The default is mapnik.
	 * 
	 * @param mapTile String that is the name of the tile provider
	 * @return ITileSource with the selected Tile-Source
	 */
	private ITileSource selectMapTile(String mapTile) {
		try {
			Field f = TileSourceFactory.class.getField(mapTile);
			return (ITileSource) f.get(null); 
		} catch (Exception e) {
			Log.e(TAG, "Invalid tile source '"+mapTile+"'", e);
			return TileSourceFactory.MAPNIK;
		}
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENT_ZOOM, osmView.getZoomLevel());
		outState.putInt(CURRENT_SCROLL_X, osmView.getScrollX());
		outState.putInt(CURRENT_SCROLL_Y, osmView.getScrollY());
		outState.putBoolean(CURRENT_CENTER_TO_GPS_POS, centerToGpsPos);
		outState.putBoolean(CURRENT_ZOOMED_TO_TRACK, zoomedToTrackAlready);
		super.onSaveInstanceState(outState);
	}


	@Override
	protected void onResume() {
		
		// setKeepScreenOn depending on user's preferences
		osmView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
		
		// Register content observer for any trackpoint changes
		getContentResolver().registerContentObserver(
				TrackContentProvider.trackPointsUri(currentTrackId),
				true, trackpointContentObserver);
		
		// Forget the last waypoint read from the DB
		// This ensures that all waypoints for the track will be reloaded 
		// from the database to populate the path layout
		lastTrackPointIdProcessed = null;
		
		// Reload path
		pathChanged();

		selectTileSource();
		
		// Refresh way points
		// wayPointsOverlay.refresh();
		
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// Unregister content observer
		getContentResolver().unregisterContentObserver(trackpointContentObserver);
		
		// Clear the points list.
		pathOverlay.clearPath();

		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();

		// Save zoom level in shared preferences
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(LAST_ZOOM, osmView.getZoomLevel());
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		 inflater.inflate(R.menu.displaytrackmap_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.displaytrackmap_menu_center_to_gps).setEnabled( (!centerToGpsPos && currentPosition != null ) );
		return super.onPrepareOptionsMenu(menu);
	}

	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.displaytrackmap_menu_center_to_gps:
			centerToGpsPos = true;
			if(currentPosition != null){
				osmViewController.animateTo(currentPosition);
			}
			break;
		case R.id.displaytrackmap_menu_settings:
			// Start settings activity
			startActivity(new Intent(this, Preferences.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch(event.getAction()){
			case MotionEvent.ACTION_MOVE:
				if (currentPosition != null)
					centerToGpsPos = false;
				break;
		}
		return super.onTouchEvent(event);
	}


	/**
	 * Creates overlays over the OSM view
	 */
	private void createOverlays() {
		pathOverlay = new PathOverlay(Color.BLUE, this);
		osmView.getOverlays().add(pathOverlay);
		
		myLocationOverlay = new SimpleLocationOverlay(this);
		osmView.getOverlays().add(myLocationOverlay);
		
		wayPointsOverlay = new WayPointsOverlay(this, currentTrackId);
		osmView.getOverlays().add(wayPointsOverlay);
	}
	
	/**
	 * On track path changed, update the two overlays and repaint view.
	 * If {@link #lastTrackPointIdProcessed} is null, this is the initial call
	 * from {@link #onResume()}, and not the periodic call from
	 * {@link ContentObserver#onChange(boolean) trackpointContentObserver.onChange(boolean)}
	 * while recording.
	 */
	private void pathChanged() {
		if (isFinishing()) {
			return;
		}
		
		// See if the track is active.
		// If not, we'll calculate initial track bounds
		// while retrieving from the database.
		// (the first point will overwrite these lat/lon bounds.)
		boolean doInitialBoundsCalc = false;
		double minLat = 91.0, minLon = 181.0;
		double maxLat = -91.0, maxLon = -181.0;
		if ((! zoomedToTrackAlready) && (lastTrackPointIdProcessed == null)) {
			final String[] proj_active = {Schema.COL_ACTIVE};
			Cursor cursor = getContentResolver().query(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, currentTrackId),
				proj_active, null, null, null);
			if (cursor.moveToFirst()) {
				doInitialBoundsCalc =
					(cursor.getInt(cursor.getColumnIndex(Schema.COL_ACTIVE)) == Schema.VAL_TRACK_INACTIVE);
			}
			cursor.close();
		}
		
		// Projection: The columns to retrieve. Here, we want the latitude, 
		// longitude and primary key only
		String[] projection = {Schema.COL_LATITUDE, Schema.COL_LONGITUDE, Schema.COL_ID};
		// Selection: The where clause to use
		String selection = null;
		// SelectionArgs: The parameter replacements to use for the '?' in the selection		
		String[] selectionArgs = null;
		
		  // Only request the track points that we have not seen yet
		// If we have processed any track points in this session then
		// lastTrackPointIdProcessed will not be null. We only want 
		// to see data from rows with a primary key greater than lastTrackPointIdProcessed  
		if (lastTrackPointIdProcessed != null) {
			selection = TrackContentProvider.Schema.COL_ID + " > ?";
			List<String> selectionArgsList  = new ArrayList<String>();
			selectionArgsList.add(lastTrackPointIdProcessed.toString());
			
			selectionArgs = selectionArgsList.toArray(new String[1]); 
		}

		// Retrieve any points we have not yet seen
		Cursor c = getContentResolver().query(
			TrackContentProvider.trackPointsUri(currentTrackId),
			projection, selection, selectionArgs, Schema.COL_ID + " asc");
		
		int numberOfPointsRetrieved = c.getCount();
		if (numberOfPointsRetrieved > 0 ) {
			c.moveToFirst();
			double lastLat = 0;
			double lastLon = 0;
			int primaryKeyColumnIndex = c.getColumnIndex(Schema.COL_ID);
			int latitudeColumnIndex = c.getColumnIndex(Schema.COL_LATITUDE);
			int longitudeColumnIndex = c.getColumnIndex(Schema.COL_LONGITUDE);
		
			// Add each new point to the track
			while(!c.isAfterLast()) {			
				lastLat = c.getDouble(latitudeColumnIndex);
				lastLon = c.getDouble(longitudeColumnIndex);
				lastTrackPointIdProcessed = c.getInt(primaryKeyColumnIndex);
				pathOverlay.addPoint((int)(lastLat * 1e6), (int)(lastLon * 1e6));
				if (doInitialBoundsCalc) {
					if (lastLat < minLat)  minLat = lastLat;
					if (lastLon < minLon)  minLon = lastLon;
					if (lastLat > maxLat)  maxLat = lastLat;
					if (lastLon > maxLon)  maxLon = lastLon;
				}
				c.moveToNext();
			}		
		
			// Last point is current position.
			currentPosition = new GeoPoint(lastLat, lastLon); 
			myLocationOverlay.setLocation(currentPosition);		
			if(centerToGpsPos) {
				osmViewController.setCenter(currentPosition);
			}
		
			// Repaint
			osmView.invalidate();
			if (doInitialBoundsCalc && (numberOfPointsRetrieved > 1)) {
				// osmdroid-3.0.8 hangs if we directly call zoomToSpan during initial onResume,
				// so post a Runnable instead for after it's done initializing.
				final double north = maxLat, east = maxLon, south = minLat, west = minLon;
				osmView.post(new Runnable() {
					@Override
					public void run() {
						osmViewController.zoomToSpan(new BoundingBoxE6(north, east, south, west));
						osmViewController.setCenter(new GeoPoint((north + south) / 2, (east + west) / 2));
						zoomedToTrackAlready = true;
					}
				});
			}
		}
		c.close();
	}
}
