package net.osmtracker.data;

import net.osmtracker.db.model.Track;

public class TrackMocks {

    // TrackId for mock Track to generate file real-track.gpx
    public static final long GPX_TRACKID = 7l;


    /**
     * This track matches the data of real-track.gpx
     *
     * @return Track
     */
    public static Track getMockTrackForGPX() {
        Track track = new Track();
        track.setTrackId(GPX_TRACKID);
        track.setName("2020-12-30_17-20-17");
        track.setDescription(null);
        track.setTags("osmtracker");
        track.setVisibility(Track.OSMVisibility.valueOf("Private"));
        track.setStartDate(1609370417227l);
        //TODO: check why TrackDate is used as startDate in Track.build)()
        track.setTrackDate(1609370417227l);

        track.setTpCount(TrackPointMocks.GPX_TRACKPOINTS_COUNT);
        track.setWpCount(WayPointMocks.GPX_WAYPOINTS_COUNT);

        return track;
    }


    /**
     *
     * @param trackName
     * @param trackStartDate
     * @return
     */
    public static Track createMockTrack(String trackName, long trackStartDate) {
        Track track = new Track();
        track.setName(trackName);
        track.setStartDate(trackStartDate);
        return track;
    }

}
