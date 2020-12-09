package net.osmtracker.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class MercatorProjectionTest {

    @Test
    public void MercatorProjection() {
        MercatorProjection mercatorProjection = new MercatorProjection(0, 0, 200, 200, 50, 50);

        assertEquals(50, mercatorProjection.getHeight());
        assertEquals(50, mercatorProjection.getWidth());

    }

    @Test
    public void project() {
        MercatorProjection mercatorProjection = new MercatorProjection(0, 0, 200, 200, 50, 50);

        int[] result = new int[2];
        int[] expected = new int[2];

        double width = mercatorProjection.getWidth();
        double height = mercatorProjection.getHeight();
        double topX = mercatorProjection.getTopX();
        double topY = mercatorProjection.getTopY();
        double dimX = mercatorProjection.getDimX();
        double dimY = mercatorProjection.getDimY();

        expected = mercatorProjection.project(10, 10);
        result[mercatorProjection.X] = (int) Math.round(((mercatorProjection.convertLongitude(10) - topX) / dimX) * width);
        result[mercatorProjection.Y] = (int) Math.round(height - (((mercatorProjection.convertLatitude(10) - topY) / dimY) * height));

        assertArrayEquals(result, expected);
    }

    @Test
    public void convertLongitude() {
        MercatorProjection mercatorProjection = new MercatorProjection(0, 0, 200, 200, 50, 50);

        double longitude = 10;
        double result = mercatorProjection.convertLongitude(longitude);

        assertEquals(longitude, result, 0.0001);
    }

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
