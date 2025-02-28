package net.osmtracker.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ArrayUtilsTest {

	double[][] arrayAsc = {{1, 1, 1}, {2, 2, 2}, {3, 3, 3}};

	double[][] arrayDesc = {{3, 3, 3}, {2, 2, 2}, {1, 1, 1}};

	@Test
	public void findMinAsc() {
		double min = ArrayUtils.findMin(arrayAsc, 0);
		assertTrue(min == 1);
	}

	@Test
	public void findMinDesc() {
		double min = ArrayUtils.findMin(arrayDesc, 2);
		assertTrue(min == 1);
	}

	@Test
	public void findMaxAsc() {
		double max = ArrayUtils.findMax(arrayAsc, 0);
		assertTrue(max == 3);
	}

	@Test
	public void findMaxDesc() {
		double max = ArrayUtils.findMax(arrayDesc, 2);
		assertTrue(max == 3);
	}
}