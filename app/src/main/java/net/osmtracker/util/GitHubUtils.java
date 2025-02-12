package net.osmtracker.util;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for interacting with the GitHub API.
 * Provides methods to retrieve file information and manage file uploads.
 *
 * <p>This class includes methods to:
 * <ul>
 *   <li>Generate a unique filename within a GitHub repository.</li>
 *   <li>Retrieve the SHA hash of a file stored in a GitHub repository.</li>
 * </ul>
 * </p>
 *
 * <p>It requires a valid GitHub authentication token for API requests.</p>
 */
public class GitHubUtils {
    /**
     * Retrieves the SHA hash of a file in a GitHub repository.
     * If the file does not exist, it returns {@code null}.
     *
     * @param repoOwner   The owner of the repository (user or organization).
     * @param repoName    The name of the GitHub repository.
     * @param repoFilePath The file path in the repository.
     * @param token       The GitHub authentication token with appropriate permissions.
     * @return The SHA hash of the file if it exists, or {@code null} if the file is not found.
     * @throws IOException   If an I/O error occurs while making the request.
     * @throws JSONException If an error occurs while parsing the JSON response.
     */
    public static void getFileSHAAsync(String repoOwner, String repoName, String repoFilePath, String token, Callback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String apiUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/contents/" + repoFilePath;
                    System.out.println("Fetching SHA: " + apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        return jsonResponse.getString("sha");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String sha) {
                callback.onResult(sha); // Return result via callback
            }
        }.execute();
    }

    /**
     * Asynchronously generates a unique filename in a GitHub repository.
     * If the file already exists, a number is appended before the extension.
     *
     * @param repoOwner   The owner of the repository.
     * @param repoName    The name of the GitHub repository.
     * @param repoFilePath The initial file path in the repository.
     * @param token       The GitHub authentication token.
     * @param callback    Callback to return the generated filename.
     */
    public static void getGHFilenameAsync(String repoOwner, String repoName, final String repoFilePath, String token, Callback callback) {
        String filename = repoFilePath.substring(0, repoFilePath.lastIndexOf("."));
        String extension = repoFilePath.substring(repoFilePath.lastIndexOf("."));
        checkFileExists(repoOwner, repoName, filename, extension, 0, token, callback);
    }

    /**
     * Recursively checks if a file exists and generates a unique filename.
     *
     * @param repoOwner  The owner of the repository.
     * @param repoName   The GitHub repository name.
     * @param filename   The base filename (without extension).
     * @param extension  The file extension.
     * @param count      The current attempt number for uniqueness.
     * @param token      The GitHub authentication token.
     * @param callback   Callback to return the final unique filename.
     */
    private static void checkFileExists(String repoOwner, String repoName, String filename, String extension, int count, String token, Callback callback) {
        String newFilename;// (count == 0) ? filename + extension : filename + "(" + count + ")" + extension;
        if (count == 0) {
            newFilename = filename + extension;
        } else {
            newFilename = filename + "(" + count + ")" + extension;
        }

        getFileSHAAsync(repoOwner, repoName, newFilename, token, new Callback() {
            @Override
            public String onResult(String sha) {
                if (sha == null) {
                    // File does not exist, return the new unique filename
                    callback.onResult(newFilename);
                } else {
                    // File exists, recursively try with the next count
                    checkFileExists(repoOwner, repoName, filename, extension, count + 1, token, callback);
                }
                return null;
            }
        });
    }
}
