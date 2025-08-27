package net.osmtracker;

import net.osmtracker.github.*;

import android.content.Context;
import android.content.SharedPreferences;

public class GitHubUser {
    private SharedPreferences sharedPreferences;

    public GitHubUser(Context context) {
        sharedPreferences = context.getSharedPreferences(GitHubConstants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveCredentials(String username, String token) {
        sharedPreferences.edit()
                .putString(GitHubConstants.KEY_USERNAME, username)
                .putString(GitHubConstants.KEY_TOKEN, token)
                .apply();
    }

    public String getUsername() {
        return sharedPreferences.getString(GitHubConstants.KEY_USERNAME, "");
    }

    public String getToken() {
        return sharedPreferences.getString(GitHubConstants.KEY_TOKEN, "");
    }

    public boolean hasCredentials() {
        return !getUsername().isEmpty() && getToken().length() == 40;
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}
