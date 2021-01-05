package net.osmtracker.data;

import net.osmtracker.db.model.TrackPoint;
import net.osmtracker.db.model.WayPoint;

public class TrackPointMocks {

    // TrackPoints of real-track.gpx
    static final int GPX_TRACKPOINTS_COUNT = 2;

    //
    public static String MOCK_TRACKPOINT_XML_A =
            "\t\t\t<trkpt lat=\"10.037569043664794\" lon=\"-84.21228868248848\">\n"
                    +"\t\t\t\t<ele>1015.1315925326198</ele>\n"
                    +"\t\t\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<speed>0.3619388937950134</speed>\n"
                    +"\t\t\t\t\t<compass>204.690002441406</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t\t\t</trkpt>\n";

    public static TrackPoint getMockTrackPointForXML(){
        TrackPoint trkpt = new TrackPoint();
        trkpt.setId(3);
        trkpt.setTrackId(7);
        trkpt.setLatitude(10.037569043664794);
        trkpt.setLongitude(-84.21228868248848);
        trkpt.setSpeed(0.3619388937950134);
        trkpt.setElevation(1015.1315925326198);
        trkpt.setAccuracy(null);
        trkpt.setPointTimestamp(990055228011l);
        trkpt.setCompassHeading(204.690002441406);
        trkpt.setCompassAccuracy(2.0d);
        trkpt.setAtmosphericPressure(null);
        return trkpt;
    }

    /**
     * This trackPoints match the data of real-track.gpx
     *
     * @param trkptId
     * @return
     */
    public static TrackPoint getMockTrackPointForGPX(Integer trkptId) {
        TrackPoint trkpt = new TrackPoint();
        trkpt.setId(trkptId);
        trkpt.setTrackId(7);

        if (trkptId == 1) {
            trkpt.setLatitude(10.0375690436648);
            trkpt.setLongitude(-84.2122886824885);
            trkpt.setSpeed(0.361938893795013);
            trkpt.setElevation(1015.13159253262);
            trkpt.setAccuracy(35.635440826416);
            trkpt.setPointTimestamp(990055225811l);
            trkpt.setCompassHeading(204.690002441406);
            trkpt.setCompassAccuracy(2d);
        } else if (trkptId == 2) {
            trkpt.setLatitude(10.0377106969496);
            trkpt.setLongitude(-84.2123268390527);
            trkpt.setSpeed(0.271533757448196);
            trkpt.setElevation(1007.3209409276);
            trkpt.setAccuracy(52.0706405639648);
            trkpt.setPointTimestamp(990055226011l);
            trkpt.setCompassHeading(204.360000610352);
            trkpt.setCompassAccuracy(2d);
        }
        trkpt.setAtmosphericPressure(null);

        return trkpt;
    }
}
