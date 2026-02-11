package net.osmtracker.util;

/**
 * A generic callback interface used for asynchronous operations.
 * Implementations of this interface should define how to handle the result
 * of an asynchronous task.
 */
public interface Callback {
    /**
     * Called when the asynchronous operation is completed.
     * Implementations should handle the provided result accordingly.
     *
     * @param result The result of the operation, which may be {@code null} if an error occurs.
     * @return A string value that may be used by the calling function, if applicable.
     */
    String onResult(String result);
}
