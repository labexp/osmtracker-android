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
}
