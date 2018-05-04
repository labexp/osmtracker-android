package org.osmtracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.osmtracker.OSMTracker;

/**
 * Created by labexp on 13/12/17.
 */
public class URLCreator {
    /*
    Create different URLs for accessing contents using the Github API (https://developer.github.com/v3/)
    or for downloading files.
    */

    private static String API_BASE = "https://api.github.com/repos/";

    private static String RAW_CONTENT = "https://raw.githubusercontent.com/";


    /**
     * Returns the URL used to get the contents of the folder `/layouts/metadata`.
     *
     * @param context {@link Context} to lookup for the preferences values
     * @return String
     */
    public static String createMetadataDirUrl(Context context) {
    	String[] ghParams = getGithubParams(context);
        String url = API_BASE + ghParams[0] + "/" + ghParams[1]
                     + "/contents/layouts/metadata?ref=" + ghParams[2];
        return url;
    }

    /**
     * Return a URL to download a file in the `/layouts/metadata/` folder.
     *
     * @param context {@link Context} to lookup for the preferences values
     * @param layoutName The name of the layout to be included in the URL for download the file.
     * @return String
     */
    public static String createMetadataFileURL(Context context, String layoutName) {

        String layoutFileName = CustomLayoutsUtils.unconvertFileName(layoutName);

        String[] ghParams = getGithubParams(context);
        String url = RAW_CONTENT + ghParams[0] + "/" + ghParams[1] + "/" + ghParams[2]
                + "/layouts/metadata/" + layoutFileName;

        return url;
    }

    /**
     * Return a URL to download a file in the `/layouts/metadata/` folder.
     *
     * @param context {@link Context} to lookup for the preferences values
     * @param layoutName The name of the layout to be included in the URL for download the file.
     * @param iso String language code of the layout (ISO 639-1)
     * @return String
     */
    public static String createLayoutFileURL(Context context, String layoutName, String iso){
        String[] ghParams = getGithubParams(context);
        String layoutFileName = CustomLayoutsUtils.createFileName(layoutName, iso);
        String url = RAW_CONTENT + ghParams[0] + "/" + ghParams[1] + "/" + ghParams[2]
                + "/layouts/" + layoutFileName;
        return url;
    }

    /**
     * Return a URL to download icon files in the `/layouts/$layoutName/` folder.
     *
     * @param context {@link Context} to lookup for the preferences values
     * @param layoutName The name of the layout to be included in the URL
     * @return String
     */
    public static String createIconsDirUrl(Context context, String layoutName){
        String[] ghParams = getGithubParams(context);
        String iconsDirName = layoutName.replace(" ", "_");

        String url = API_BASE + ghParams[0] + "/" + ghParams[1]
                + "/contents/layouts/" + iconsDirName + "?ref=" + ghParams[2];
        return url;
    }

    /**
     * The method params describe a Github repository with custom layouts (username, repository
     * and branch). The test URL will be used to test is the repository has layouts on it. Because
     * the `metadata` folder is mandatory on the server, the URL returned is the one used for
     * getting the contents of that folder.
     *
     * @param ghUsername String
     * @param repositoryName String
     * @param branchName String
     * @return String with the test URL.
     */
    public static String createTestURL(String ghUsername, String repositoryName, String branchName){

        String url = API_BASE + ghUsername + "/" + repositoryName + "/contents/layouts/metadata?ref=" + branchName;
        return url;
    }

    /**
     * Get custom layots server configuration values from the SharedPreferences.
     *
     * @param context {@link Context} to lookup for the preferences values
     * @return String array with the following values:
     *           [0] = Github Username
     *           [1] = Repository Name
     *           [2] = Branch Name
     */
    private static String[] getGithubParams(Context context) {
        //the shared preferences file where the values are saved
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String username = preferences.getString(OSMTracker.Preferences.KEY_GITHUB_USERNAME, OSMTracker.Preferences.VAL_GITHUB_USERNAME);
        String repo = preferences.getString(OSMTracker.Preferences.KEY_REPOSITORY_NAME, OSMTracker.Preferences.VAL_REPOSITORY_NAME);
        String branch = preferences.getString(OSMTracker.Preferences.KEY_BRANCH_NAME, OSMTracker.Preferences.VAL_BRANCH_NAME);

        String[] params = {username, repo, branch};
        return params;
    }
}

