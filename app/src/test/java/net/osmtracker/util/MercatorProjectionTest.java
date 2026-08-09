package net.osmtracker.util;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class MercatorProjectionTest {

    @Parameterized.Parameter(0)
    public double minLat;

    @Parameterized.Parameter(1)
    public double lat;

    @Parameterized.Parameter(2)
    public double maxLat;

    @Parameterized.Parameter(3)
    public double minLon;

    @Parameterized.Parameter(4)
    public double lon;

    @Parameterized.Parameter(5)
    public double maxLon;

    @Parameterized.Parameter(6)
    public int expectedX;

    @Parameterized.Parameter(7)
    public int expectedY;

    @Parameterized.Parameter(8)
    public double expectedScale;

    @Parameterized.Parameter(9)
    public Float degre;

    @Parameterized.Parameter(10)
    public boolean isLat;

    @Parameterized.Parameter(11)
    public String expectedDms;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { -89, -83.83, -80, /**/ -180, -171.171, -160, /**/ 323, 879, /**/ 0.0315, -83.83f,  true, "83° 49' 48\" S" },
            { -45, -42.45, -40, /**/  170,  175.175,  180, /**/ 373, 630, /**/ 0.0138, -42.45f, false, "42° 27' 0\" W" },
            {  45,  48.48,  50, /**/ -160, -151.151, -150, /**/ 637, 541, /**/ 0.0138,  48.48f,  true, "48° 28' 47\" N" },
            {  80,  82.82,  85, /**/  110,  111.111,  120, /**/ 235, 668, /**/ 0.0311,  82.82f, false, "82° 49' 11\" E" },
            {  89,  89.89,  90, /**/  111,  111.111,  112, /**/  80, 640, /**/ 0.0013,    null, true, "" }
        });
    }

    @Test
    public void testProject() {
        MercatorProjection projection = new MercatorProjection(minLat, minLon, maxLat, maxLon, 720, 1280);
        int[] point = projection.project(lon, lat);
        assertNotNull(point);
        assertEquals(expectedX, point[MercatorProjection.X]);
        assertEquals(expectedY, point[MercatorProjection.Y]);
        assertEquals(expectedScale, projection.getScale(), 0.0001);
    }

    @Test
    public void testFormatDegreesAsDMS() {
        String formattedDms = MercatorProjection.formatDegreesAsDMS(degre, isLat);
        assertEquals(expectedDms, formattedDms);
    }

}
