package net.osmtracker;

public class GitHubUser {

    private int id;
    private String username;
    private String token;

    public GitHubUser() {
        setId(-1);
        setUsername("");
        setToken("");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
