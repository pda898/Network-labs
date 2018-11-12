package ru.nsu.ccfit;

public class Message {
    private int id;
    private String message;
    private long authorID;

    public Message(int id, String message, long authorID) {
        this.id = id;
        this.message = message;
        this.authorID = authorID;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public long getAuthorID() {
        return authorID;
    }

}
