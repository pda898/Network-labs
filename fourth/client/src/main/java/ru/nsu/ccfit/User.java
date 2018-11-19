package ru.nsu.ccfit;

public class User {
    private long id;
    private String username;
    private Boolean online;

    public User(long id, String name) {
        this.id = id;
        this.username = name;
        this.online = true;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Boolean isAlive() {
        if (online == null) return null;
        return online;
    }
}
