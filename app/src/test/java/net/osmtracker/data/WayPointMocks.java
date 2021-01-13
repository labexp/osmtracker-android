package net.osmtracker.data;


import net.osmtracker.db.model.WayPoint;

import java.util.Date;

import static net.osmtracker.util.UnitTestUtils.createDateFrom;

public class WayPointMocks {

    // WayPoints of gpx-test.gpx
    static final int GPX_WAYPOINTS_COUNT = 1;

    //compass = none, accuracy = none. hdop = false
    public static String MOCK_WAYPOINT_XML_A =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t</wpt>\n";

    //compass = none, accuracy = name. hdop = false
    public static String MOCK_WAYPOINT_XML_B =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto (24.0m)]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t</wpt>\n";

    //compass = none, accuracy = comment. hdop = false
    public static String MOCK_WAYPOINT_XML_C =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<cmt><![CDATA[Precisión: 24.0m]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t</wpt>\n";

    //compass = comment, accuracy = none. hdop = false
    public static String MOCK_WAYPOINT_XML_D =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<cmt><![CDATA[compass heading: 271.4599914550781deg\n"
                    +"\t\t\tcompass accuracy: 2.0]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t</wpt>\n";

    //compass = comment, accuracy = name. hdop = false
    public static String MOCK_WAYPOINT_XML_E =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto (24.0m)]]></name>\n"
                    +"\t\t<cmt><![CDATA[compass heading: 271.4599914550781deg\n"
                    +"\t\t\tcompass accuracy: 2.0]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t</wpt>\n";

    //compass = comment, accuracy = comment. hdop = false
    public static String MOCK_WAYPOINT_XML_F =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<cmt><![CDATA[Precisión: 24.0m\n"
                    +"\t\t\tcompass heading: 271.4599914550781deg\n"
                    +"\t\t\tcompass accuracy: 2.0]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t</wpt>\n";

    //compass = extension, accuracy = none. hdop = false
    public static String MOCK_WAYPOINT_XML_G =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<compass>271.4599914550781</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t</wpt>\n";

    //compass = extension, accuracy = name. hdop = false
    public static String MOCK_WAYPOINT_XML_H =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto (24.0m)]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<compass>271.4599914550781</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t</wpt>\n";

    //compass = extension, accuracy = comment. hdop = false
    public static String MOCK_WAYPOINT_XML_I =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<cmt><![CDATA[Precisión: 24.0m]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<compass>271.4599914550781</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t</wpt>\n";

    //compass = none, accuracy = none. hdop = true
    public static String MOCK_WAYPOINT_XML_J =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t</wpt>\n";

    //compass = none, accuracy = name. hdop = true
    public static String MOCK_WAYPOINT_XML_K =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto (24.0m)]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t</wpt>\n";

    //compass = none, accuracy = comment. hdop = true
    public static String MOCK_WAYPOINT_XML_L =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<cmt><![CDATA[Precisión: 24.0m]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t</wpt>\n";

    //compass = comment, accuracy = none. hdop = true
    public static String MOCK_WAYPOINT_XML_M =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<cmt><![CDATA[compass heading: 271.4599914550781deg\n"
                    +"\t\t\tcompass accuracy: 2.0]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t</wpt>\n";

    //compass = comment, accuracy = name. hdop = true
    public static String MOCK_WAYPOINT_XML_N =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto (24.0m)]]></name>\n"
                    +"\t\t<cmt><![CDATA[compass heading: 271.4599914550781deg\n"
                    +"\t\t\tcompass accuracy: 2.0]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t</wpt>\n";

    //compass = comment, accuracy = comment. hdop = true
    public static String MOCK_WAYPOINT_XML_O =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<cmt><![CDATA[Precisión: 24.0m\n"
                    +"\t\t\tcompass heading: 271.4599914550781deg\n"
                    +"\t\t\tcompass accuracy: 2.0]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t</wpt>\n";

    //compass = extension, accuracy = none.  hdop = true
    public static String MOCK_WAYPOINT_XML_P =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<compass>271.4599914550781</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t</wpt>\n";

    //compass = extension, accuracy = name. hdop = true
    public static String MOCK_WAYPOINT_XML_Q =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto (24.0m)]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<compass>271.4599914550781</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t</wpt>\n";

    //compass = extension, accuracy = comment. hdop = true
    public static String MOCK_WAYPOINT_XML_R =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<cmt><![CDATA[Precisión: 24.0m]]></cmt>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t<hdop>6.0</hdop>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<compass>271.4599914550781</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t</wpt>\n";

    //compass = extension, accuracy = comment. hdop = true
    //wpt.accuracy == null and wpt.atmosphericPressure == 5.54321
    public static String MOCK_WAYPOINT_XML_S =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<compass>271.4599914550781</compass>\n"
                    +"\t\t\t\t\t<compass_accuracy>2.0</compass_accuracy>\n"
                    +"\t\t\t\t\t<baro>5.5</baro>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t</wpt>\n";

    //compass = extension, accuracy = comment. hdop = true
    //wpt.accuracy == null, wpt.atmosphericPressure == 5.54321, wpt.compassHeading == null
    // (but wpt.compassAccuracy != null)
    public static String MOCK_WAYPOINT_XML_T =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t\t\t\t<extensions>\n"
                    +"\t\t\t\t\t<baro>5.5</baro>\n"
                    +"\t\t\t\t</extensions>\n"
                    +"\t</wpt>\n";

    public static WayPoint getMockWayPointForXML(){
        WayPoint wpt = new WayPoint();
        wpt.setLatitude(10.037578304829676);
        wpt.setLongitude(-84.21210085275517);
        wpt.setElevation(1073.3167528454214);
        wpt.setPointTimestamp(990055228011l); //2001-05-16T23:20:28Z
        wpt.setName("Nota de texto");
        wpt.setAccuracy(24.0);
        wpt.setCompassHeading(271.4599914550781);
        wpt.setCompassAccuracy(2.0);
        wpt.setLink(null);
        wpt.setNumberOfSatellites(0);
        wpt.setAtmosphericPressure(null);
        return wpt;
    }

    /**
     * Matches data of gpx-test.gpx used in ExportTrackTest.testWriteGPXFile()
     *
     * @param wptId
     * @return
     */
    public static WayPoint getMockWayPointForGPX(Integer wptId) {
        WayPoint wpt = new WayPoint();

        wpt.setId(wptId);
        wpt.setTrackId(7);
        wpt.setUuid("2681b418-d6c6-4f7a-baf2-7c24a2a274b7");
        wpt.setLatitude(10.0375634848231);
        wpt.setLongitude(-84.2123549868801);
        wpt.setElevation(1029.89940680377);
        wpt.setAccuracy(24.0);
        wpt.setPointTimestamp(990055228011l);
        wpt.setName("Punto de prueba");
        wpt.setLink(null);
        wpt.setNumberOfSatellites(0);
        wpt.setCompassHeading(204.029998779297);
        wpt.setCompassAccuracy(2d);
        wpt.setAtmosphericPressure(null);

        return wpt;
    }

}