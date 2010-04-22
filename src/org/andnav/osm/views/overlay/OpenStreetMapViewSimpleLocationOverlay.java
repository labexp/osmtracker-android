// Created by plusminus on 22:01:11 - 29.09.2008
package org.andnav.osm.views.overlay;

import me.guillaumin.android.osmtracker.R;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapView.OpenStreetMapViewProjection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

/**
 * 
 * @author Nicolas Gramlich
 *
 */
public class OpenStreetMapViewSimpleLocationOverlay extends OpenStreetMapViewOverlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================
	
	protected final Paint mPaint = new Paint();
	
	protected final Bitmap PERSON_ICON;
	/** Coordinates the feet of the person are located. */
	protected final android.graphics.Point PERSON_HOTSPOT = new android.graphics.Point(24,39);
	
	protected GeoPoint mLocation;

	// ===========================================================
	// Constructors
	// ===========================================================
	
	
	public OpenStreetMapViewSimpleLocationOverlay(final Bitmap bitmap, int centerX, int centerY){
		this.PERSON_ICON = bitmap;
		this.PERSON_HOTSPOT.x = centerX;
		this.PERSON_HOTSPOT.y = centerY;
	}

	
	// ===========================================================
	// Getter & Setter
	// ===========================================================
	
	public void setLocation(final GeoPoint mp){
		this.mLocation = mp;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================
	
	@Override
	protected void onDrawFinished(Canvas c, OpenStreetMapView osmv) {
		return;
	}
	
	@Override
	public void onDraw(final Canvas c, final OpenStreetMapView osmv) {
		if(this.mLocation != null){
			final OpenStreetMapViewProjection pj = osmv.getProjection();
			final Point screenCoords = new Point();
			pj.toMapPixels(this.mLocation, screenCoords);
	
			c.drawBitmap(PERSON_ICON, screenCoords.x - PERSON_HOTSPOT.x, screenCoords.y - PERSON_HOTSPOT.y, this.mPaint);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
