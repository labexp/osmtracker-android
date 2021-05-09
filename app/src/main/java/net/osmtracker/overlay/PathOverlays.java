package net.osmtracker.overlay;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;

import android.content.Context;
import android.graphics.Color;

/**
 * Collection of Overlays, useful to draw interrupted paths
 */
public class PathOverlays {
	private final float width;
	private final Context ctx;
	private final MapView osmView;
	private final boolean[] havePoint = new boolean[] { false, false };
	
	private final int[] curIdx=new int[] { 0, 0};
	
	private final List<List<PathOverlay>> paths = new ArrayList<>();
	private final int[] colors = new int[] { Color.BLUE, Color.GREEN };
	
	private void addPath(int slot) {
		PathOverlay path = new PathOverlay(colors[slot], width, ctx);
		paths.get(slot).add(path);
		osmView.getOverlays().add(path);
	}

	public void clearPath() {
		for(int slot=0; slot<2; slot++) {
			for(PathOverlay path : paths.get(slot))
				path.clearPath();
			curIdx[slot]=0;
		}
	}

	public PathOverlays(float width,
			    Context ctx, MapView osmView) {
		this.width=width;
		this.ctx=ctx;
		this.osmView = osmView;
		for(int slot=0; slot<2; slot++)
			paths.add(new ArrayList<>());
	}

	public void addPoint(double lat, double lon, boolean isRoute) {
		int slot= isRoute ? 1 : 0;
		if(curIdx[slot] >= paths.get(slot).size())
			addPath(slot);
		paths.get(slot).get(curIdx[slot]).addPoint(lat, lon);
		havePoint[slot]=true;
	}

	public void nextSegment(boolean isRoute) {
		int slot= isRoute ? 1 : 0;
		if(havePoint[slot])
			curIdx[slot]++;
		havePoint[slot]=false;
	}
}
