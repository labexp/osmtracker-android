package net.osmtracker.data;


import net.osmtracker.db.model.WayPoint;

public class WayPointMocks {

    // WayPoints of real-track.gpx
    static final int GPX_WAYPOINTS_COUNT = 1;

    //compass = none, accuracy= none.
    public static String MOCK_WAYPOINT_XML_A =
            "\t<wpt lat=\"10.037578304829676\" lon=\"-84.21210085275517\">\n"
                    +"\t\t<ele>1073.3167528454214</ele>\n"
                    +"\t\t<time>2001-05-16T23:20:28Z</time>\n"
                    +"\t\t<name><![CDATA[Nota de texto]]></name>\n"
                    +"\t\t<sat>0</sat>\n"
                    +"\t</wpt>\n";

    public static WayPoint getMockWayPointForXML(){
        WayPoint wpt = new WayPoint();
        wpt.setLatitude(10.037578304829676);
        wpt.setLongitude(-84.21210085275517);
        wpt.setElevation(1073.3167528454214);
        wpt.setPointTimestamp(990055228011l);
        wpt.setName("Nota de texto");
        wpt.setAccuracy(0.0);
        wpt.setCompassHeading(0.0);
        wpt.setCompassHeading(0.0);
        wpt.setLink(null);
        wpt.setNumberOfSatellites(0);
        wpt.setAtmosphericPressure(null);
        return wpt;
    }

    /**
     * This wayPoint matche data of real-track.gpx
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