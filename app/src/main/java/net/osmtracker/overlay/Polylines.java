package net.osmtracker.overlay;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import android.graphics.Paint;

/**
 * Collection of Polylines, useful to draw interrupted paths
 */
public class Polylines {
	private int color;
	private float width;
	private MapView osmView;
	private boolean havePoint;
	
	private int curIdx=0;
	
	private List<Polyline> polylines = new ArrayList<Polyline>();

	private void addPolyline() {
		Polyline polyline = new Polyline();
		Paint paint = polyline.getOutlinePaint();
                paint.setColor(color);
                paint.setStrokeWidth(width);

		polylines.add(polyline);
		osmView.getOverlayManager().add(polyline);
	}

	public void clear() {
		for(Polyline polyline : polylines)
			polyline.setPoints(new ArrayList<>());
		curIdx=0;
	}

	public Polylines(int color, float width, MapView osmView) {
		this.color=color;
		this.width=width;
		this.osmView = osmView;
		addPolyline();
		havePoint=false;
	}

	public void addPoint(GeoPoint gp) {
		if(curIdx >= polylines.size())
			addPolyline();
		polylines.get(curIdx).addPoint(gp);
		havePoint=true;
	}

	public void nextSegment() {
		if(havePoint)
			curIdx++;
		havePoint=false;
	}
}
