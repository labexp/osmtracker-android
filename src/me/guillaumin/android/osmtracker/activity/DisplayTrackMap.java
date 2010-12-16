package me.guillaumin.android.osmtracker.activity;

import me.guillaumin.android.osmtracker.R;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;

import org.andnav.osm.contributor.util.constants.OpenStreetMapContributorConstants;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapViewController;
import org.andnav.osm.views.overlay.OpenStreetMapViewPathOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewSimpleLocationOverlay;
import org.andnav.osm.views.util.IOpenStreetMapRendererInfo;
import org.andnav.osm.views.util.OpenStreetMapRendererBase;
import org.andnav.osm.views.util.OpenStreetMapRendererFactory;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Display current track over an OSM map.
 * Based on osmdroid code http://osmdroid.googlecode.com/
 * 
 * @author Viesturs Zarins
 *
 */
public class DisplayTrackMap extends Activity implements OpenStreetMapContributorConstants{

	@SuppressWarnings("unused")
	private static final String TAG = DisplayTrackMap.class.getSimpleName();
	
	/**
	 * Key for keeping the zoom level in the saved instance bundle
	 */
	private static final String CURRENT_ZOOM = "currentZoom";
	
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
	private OpenStreetMapView osmView;
	
	/**
	 * Controller to interact with view
	 */
	private OpenStreetMapViewController osmViewController;
	
	/**
	 * OSM view overlay that displays current location
	 */
	private OpenStreetMapViewSimpleLocationOverlay myLocationOverlay;
	
	/**
	 * OSM view overlay that displays current path
	 */
	private OpenStreetMapViewPathOverlay pathOverlay;
	
	/**
	 * Current track id
	 */
	private long currentTrackId;
	
	/**
	 * whether the map display should be centered to the gps location 
	 */
	private boolean centerToGpsPos = true;
	
	/**
	 * the last position we know
	 */
	private GeoPoint currentPosition;

	/**
	 * Observes changes on trackpoints
	 */
	private ContentObserver trackpointContentObserver;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.displaytrackmap);
        
        currentTrackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
        setTitle(getTitle() + ": #" + currentTrackId);
        
        // Initialize OSM view
        osmView = (OpenStreetMapView) findViewById(R.id.displaytrackmap_osmView);
        osmViewController = osmView.getController();
        
        // Check if there is a saved zoom level
        if(savedInstanceState != null) {
        	osmViewController.setZoom(savedInstanceState.getInt(CURRENT_ZOOM, DEFAULT_ZOOM));
        } else {
        	// Try to get last zoom Level from Shared Preferences
        	SharedPreferences settings = getPreferences(MODE_PRIVATE);
        	osmViewController.setZoom(settings.getInt(LAST_ZOOM, DEFAULT_ZOOM));
        }
        
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
    
    
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENT_ZOOM, osmView.getZoomLevel());
		super.onSaveInstanceState(outState);
	}


	@Override
	protected void onResume() {
		// Register content observer for any trackpoint changes
		getContentResolver().registerContentObserver(
				TrackContentProvider.trackPointsUri(currentTrackId),
				true, trackpointContentObserver);
		
        // Reload path
        pathChanged();
        
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
				osmViewController.setCenter(currentPosition);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch(event.getAction()){
			case MotionEvent.ACTION_MOVE:
				centerToGpsPos = false;
				break;
		}
		return super.onTouchEvent(event);
	}


	/**
	 * Creates overlays over the OSM view
	 */
	private void createOverlays() {
        pathOverlay = new OpenStreetMapViewPathOverlay(Color.BLUE, this);
        osmView.getOverlays().add(pathOverlay);
        
        myLocationOverlay = new OpenStreetMapViewSimpleLocationOverlay(this);
        osmView.getOverlays().add(myLocationOverlay);
	}
	
	/**
	 * On track path changed, update the two overlays and repaint view.
	 */
	private void pathChanged() {
		if (isFinishing()) {
			return;
		}
		
		// Update only the new points
		Cursor c = getContentResolver().query(
				TrackContentProvider.trackPointsUri(currentTrackId),
				null, null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " asc");
		int existingPoints = pathOverlay.getNumberOfPoints();
		
		// Process only if we have data, and new data only
		if (c.getCount() > 0 &&  c.getCount() > existingPoints) {		
			c.moveToPosition(existingPoints);
			double lastLat = 0;
			double lastLon = 0;
		
			// Add each new point to the track
			while(!c.isAfterLast()) {			
				lastLat = c.getDouble(c.getColumnIndex(Schema.COL_LATITUDE));
				lastLon = c.getDouble(c.getColumnIndex(Schema.COL_LONGITUDE));
				pathOverlay.addPoint((int)(lastLat * 1e6), (int)(lastLon * 1e6));
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
		}
		c.close();
	}
}