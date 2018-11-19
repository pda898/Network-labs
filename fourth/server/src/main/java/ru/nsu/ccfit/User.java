package ru.nsu.ccfit;

public class User {
    private long id;
    private String username;
    private Boolean online;
    private transient Long lastseen;
    private transient static final long TIMEOUT = 1*60*1000L;

    public User(long id, String name) {
        this.id = id;
        this.username = name;
        this.online = true;
        this.lastseen = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Boolean isOnline() {
        if (online == null) return null;
        return online;
    }

    public void update() {
        lastseen = System.currentTimeMillis();
    }

    public void check() {
        if (System.currentTimeMillis() - lastseen > TIMEOUT) {
            online = null;
        }
    }

    public void setOffline() {
        this.online = false;
    }
}
