package net.osmtracker.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class MercatorProjectionTest {

    @Test
    public void convertLatitude() {
        double latitude = 45.43f;
        double result = MercatorProjection.convertLatitude(latitude);
        double resultExpected = Math.log(Math.tan(Math.PI / 4 + (45.43f * Math.PI / 180 / 2))) / (Math.PI / 180);
        assertEquals(result, resultExpected, 0.00001);
    }

    @Test
    public void formatDegreesAsDMS() {
        float degrees = (float) 43.0438;
        boolean isLatitude = true;
        String resultExpected = "43° 2' 37\" N";
        String result = MercatorProjection.formatDegreesAsDMS(degrees, isLatitude);
        assertEquals(resultExpected, result);
    }
}
