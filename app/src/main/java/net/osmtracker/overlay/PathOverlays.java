package net.osmtracker.overlay;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;

import android.content.Context;

/**
 * Collection of Overlays, useful to draw interrupted paths
 */
public class PathOverlays {
	private int color;
	private float width;
	private Context ctx;
	private MapView osmView;
	private boolean havePoint;
	
	private int curIdx=0;
	
	private List<PathOverlay> paths = new ArrayList<PathOverlay>();

	private void addPath() {
		PathOverlay path = new PathOverlay(color, width, ctx);
		paths.add(path);
		osmView.getOverlays().add(path);
	}

	public void clearPath() {
		for(PathOverlay path : paths)
			path.clearPath();
		curIdx=0;
	}

	public PathOverlays(int color, float width,
			    Context ctx, MapView osmView) {
		this.color=color;
		this.width=width;
		this.ctx=ctx;
		this.osmView = osmView;
		addPath();
		havePoint=false;
	}

	public void addPoint(double lat, double lon) {
		if(curIdx >= paths.size())
			addPath();
		paths.get(curIdx).addPoint(lat, lon);
		havePoint=true;
	}

	public void nextSegment() {
		if(havePoint)
			curIdx++;
		havePoint=false;
	}
}
