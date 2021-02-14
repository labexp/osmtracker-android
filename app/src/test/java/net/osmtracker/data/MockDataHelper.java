package net.osmtracker.data;

import android.content.Context;

import net.osmtracker.db.DataHelper;
import net.osmtracker.db.model.Track;
import net.osmtracker.db.model.TrackPoint;
import net.osmtracker.db.model.WayPoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MockDataHelper extends DataHelper {

    public MockDataHelper(Context context) {
        super(context);
    }

    public void setTrackExportDate(long trackId, long exportTime){
        //ok
    }


    public Track getTrackByStartDate(Date startDate) {
        return TrackMocks.getMockTrackForGPX();
    }

    public Track getTrackById(long trackId) {
        return TrackMocks.getMockTrackForGPX();
    }

    public List<Integer> getWayPointIdsOfTrack(long trackId) {
        List<Integer> out = new ArrayList<Integer>();
        int i = 1;
        for(; i <= WayPointMocks.GPX_WAYPOINTS_COUNT; i++) {
            out.add(new Integer(i));
        }
        return out;
    }

    public WayPoint getWayPointById(Integer wayPointId) {
        return WayPointMocks.getMockWayPointForGPX(wayPointId);
    }

    public List<Integer> getTrackPointIdsOfTrack(long trackId) {
        List<Integer> out = new ArrayList<Integer>();
        int i = 1;
        for(; i <= TrackPointMocks.GPX_TRACKPOINTS_COUNT; i++) {
            out.add(new Integer(i));
        }
        return out;
    }

    public TrackPoint getTrackPointById(Integer trackPointId) {
        return TrackPointMocks.getMockTrackPointForGPX(trackPointId);
    }
}
