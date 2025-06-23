package net.osmtracker;

import android.content.Context;
import android.content.SharedPreferences;

public class GitHubUser {

    private static final String PREF_NAME = "GitHubPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_TOKEN = "token";

    private SharedPreferences sharedPreferences;

    public GitHubUser(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveCredentials(String username, String token) {
        sharedPreferences.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_TOKEN, token)
                .apply();
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, "");
    }

    public boolean hasCredentials() {
        return !getUsername().isEmpty() && getToken().length() == 40;
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
