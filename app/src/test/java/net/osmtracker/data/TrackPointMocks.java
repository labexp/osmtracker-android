package net.osmtracker.data;

import net.osmtracker.db.model.TrackPoint;

public class TrackPointMocks {

    // TrackPoints of gpx-test.gpx
    static final int GPX_TRACKPOINTS_COUNT = 2;

    //hdop = false, compass = none
    public static String MOCK_TRACKPOINT_XML_A =
            "\t\t\t<trkpt lat=\"10.037569043664794\" lon=\"-84.21228868248848\">\n"
                    +"\t\t\t\t<ele>1015.1315925326198</ele>\n"
                    +"\t\t\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<speed>0.3619388937950134</speed>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t\t\t</trkpt>\n";

    //hdop = false, compass = comment
    public static String MOCK_TRACKPOINT_XML_B =
            "\t\t\t<trkpt lat=\"10.037569043664794\" lon=\"-84.21228868248848\">\n"
                    +"\t\t\t\t<ele>1015.1315925326198</ele>\n"
                    +"\t\t\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t\t\t<cmt><![CDATA[compass: 204.690002441406\n"
                    +"\t\t\t\t\tcompAccuracy: 2.0]]></cmt>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<speed>0.3619388937950134</speed>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t\t\t</trkpt>\n";

    //hdop = false, compass = extension
    public static String MOCK_TRACKPOINT_XML_C =
            "\t\t\t<trkpt lat=\"10.037569043664794\" lon=\"-84.21228868248848\">\n"
                    +"\t\t\t\t<ele>1015.1315925326198</ele>\n"
                    +"\t\t\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<speed>0.3619388937950134</speed>\n"
                    +"\t\t\t\t\t<compass>204.690002441406</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t\t\t</trkpt>\n";

    //hdop = true, compass = none
    public static String MOCK_TRACKPOINT_XML_D =
            "\t\t\t<trkpt lat=\"10.037569043664794\" lon=\"-84.21228868248848\">\n"
                    +"\t\t\t\t<ele>1015.1315925326198</ele>\n"
                    +"\t\t\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t\t\t<hdop>10.0</hdop>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<speed>0.3619388937950134</speed>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t\t\t</trkpt>\n";

    //hdop = true, compass = comment
    public static String MOCK_TRACKPOINT_XML_E =
            "\t\t\t<trkpt lat=\"10.037569043664794\" lon=\"-84.21228868248848\">\n"
                    +"\t\t\t\t<ele>1015.1315925326198</ele>\n"
                    +"\t\t\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t\t\t<hdop>10.0</hdop>\n"
                    +"\t\t\t\t<cmt><![CDATA[compass: 204.690002441406\n"
                    +"\t\t\t\t\tcompAccuracy: 2.0]]></cmt>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<speed>0.3619388937950134</speed>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t\t\t</trkpt>\n";

    //hdop = true, compass = extension
    public static String MOCK_TRACKPOINT_XML_F =
            "\t\t\t<trkpt lat=\"10.037569043664794\" lon=\"-84.21228868248848\">\n"
                    +"\t\t\t\t<ele>1015.1315925326198</ele>\n"
                    +"\t\t\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t\t\t<hdop>10.0</hdop>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<speed>0.3619388937950134</speed>\n"
                    +"\t\t\t\t\t<compass>204.690002441406</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t\t\t</trkpt>\n";

    //hdop = true, compass = none
    //trkpt.Accuracy = null
    public static String MOCK_TRACKPOINT_XML_G =
            "\t\t\t<trkpt lat=\"10.037569043664794\" lon=\"-84.21228868248848\">\n"
                    +"\t\t\t\t<ele>1015.1315925326198</ele>\n"
                    +"\t\t\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<speed>0.3619388937950134</speed>\n"
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
        trkpt.setAccuracy(40.0);
        trkpt.setPointTimestamp(990055228011l);
        trkpt.setCompassHeading(204.690002441406);
        trkpt.setCompassAccuracy(2.0d);
        trkpt.setAtmosphericPressure(null);
        return trkpt;
    }

    /**
     * Matches data of gpx-test.gpx used in ExportTrackTest.testWriteGPXFile()
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
