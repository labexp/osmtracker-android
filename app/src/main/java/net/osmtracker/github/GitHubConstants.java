package net.osmtracker.github;

public final class GitHubConstants {
    public static String GITHUB_API_URL = "https://api.github.com";
    public static String GITHUB_TOKENS_URL = "https://github.com/settings/tokens";
    public static String GITHUB_API_REPOS_URL = GITHUB_API_URL + "/repos/";
    public static String GITHUB_API_USER_URL = GITHUB_API_URL + "/user/";
    public static String GITHUB_API_USER_REPOS_URL = GITHUB_API_USER_URL + "/repos/";

    private GitHubConstants() {
        // Private constructor to prevent instantiation
    }
    /**
     * Builds the URL to fetch a repository's forks.
     *
     * @param username The GitHub username.
     * @param repo The repository name.
     * @return The complete URL for the forks endpoint.
     */
    public static String getRepoForksUrl(String username, String repo) {
        return GITHUB_API_REPOS_URL + username + "/" + repo + "/forks";
    }

    /**
     * Builds the URL to fetch a repository's pull
     * @param repoOrigen The repository name.
     * @return The complete URL for the pull requests endpoint.
     */
    public static String getRepoPullsUrl(String repoOrigen) {
        return GITHUB_API_REPOS_URL + repoOrigen + "/pulls";
    }

    /**
     * Builds the URL to fetch a file's content.
     * @param repoOrigen The repository name.
     * @param filename The file name.
     * @return The complete URL for the file content endpoint.
     */
    public static String getRepoFileContentUrl(String repoOrigen, String filename) {
        return GITHUB_API_REPOS_URL + repoOrigen + "/contents/" + filename;
    }

    /**
     * String fullURL = getBaseURL() + "/user/repos?" + "sort=" + sortBy ;
     * Builds the URL to fetch a user's repositories with optional sorting.
     * @param sortBy The sorting parameter.
     * @return The complete URL for the user's repositories endpoint.
     */
    public static String getUserReposUrl(String sortBy) {
        return GITHUB_API_USER_REPOS_URL + "?sort=" + sortBy;
    }
}