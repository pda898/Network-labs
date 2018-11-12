package ru.nsu.ccfit;

public class LoginInfo {
    private long id;
    private String username;
    private Boolean online;
    private String token;

    public LoginInfo(User user, String token) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.online = user.isOnline();
        this.token = token;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Boolean isOnline() {
        return online;
    }

    public String getToken() {
        return token;
    }
}
