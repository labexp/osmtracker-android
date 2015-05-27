package me.guillaumin.android.osmtracker.util;

/**
 * Array utilities.
 * 
 * @author Nicolas Guillaumin
 */
public final class ArrayUtils {

	/**
	 * Finds minimum value of an 2-dim array
	 * 
	 * @param in
	 *				Input array
	 * @param offset
	 *				Offset to use for second dimension
	 * @return minimum value of the offset column for this array
	 */
	public static double findMin(double[][] in, int offset) {
		double out = in[0][offset];
		for (int i = 0; i < in.length; i++) {
			if (in[i][offset] < out) {
				out = in[i][offset];
			}
		}
		return out;
	}

	/**
	 * Finds maximum value of an 2-dim array
	 * 
	 * @param in
	 *				Input array
	 * @param offset
	 *				Offset to use for second dimension
	 * @return maximum value of the offset column for this array
	 */
	public static double findMax(double[][] in, int offset) {
		double out = in[0][offset];
		for (int i = 0; i < in.length; i++) {
			if (in[i][offset] > out) {
				out = in[i][offset];
			}
		}
		return out;
	}

}
