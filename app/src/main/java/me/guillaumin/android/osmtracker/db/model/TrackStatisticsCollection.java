package me.guillaumin.android.osmtracker.db.model;

import me.guillaumin.android.osmtracker.db.model.TrackStatistics;
import android.content.ContentResolver;
import java.util.TreeMap;


/**
 * Collection of statistics for several tracks
 *
 * @author Arseniy Lartsev
 *
 */
public class TrackStatisticsCollection {
	/** Statistics for all existing tracks */
	private TreeMap<Long, TrackStatistics> tracksStatistics = new TreeMap<Long, TrackStatistics> ();
	
	private final ContentResolver contentResolver;

	public TrackStatisticsCollection(ContentResolver resolver) {
		contentResolver = resolver;
	}

	/**
	 * Get the latest statistics for a given track
	 * If doesn't exists, create and calculate from the database
	 */
	public TrackStatistics get(long trackId) {
		TrackStatistics stat;
		if (! tracksStatistics.containsKey(trackId)) {
			stat = new TrackStatistics(trackId, contentResolver);
			tracksStatistics.put(trackId, stat);
		} else {
			stat = tracksStatistics.get(trackId);
			stat.update();
		}
		return stat;
	}

	/**
	 * Remove statistics for a given track
	 */
	public void remove(long trackId) {
		tracksStatistics.remove(trackId);
	}
}
