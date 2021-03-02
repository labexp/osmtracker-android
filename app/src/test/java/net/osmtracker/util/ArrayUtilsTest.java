package net.osmtracker.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ArrayUtilsTest {

    double data[][] = {
            {1,1,1},
            {2,2,2},
            {3,3,3}
    };

    @Test
    public void findMin() {

        double min = ArrayUtils.findMin(data, 0);
        assertTrue(min == 1);

    }

    @Test
    public void findMax() {

        double max = ArrayUtils.findMax(data, 0);
        assertTrue(max == 3);

    }
}