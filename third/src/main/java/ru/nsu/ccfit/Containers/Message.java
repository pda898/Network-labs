package ru.nsu.ccfit.Containers;

import java.net.InetAddress;

public class Message {
    private InetAddress from;
    private int port;
    private String message;
    private char type;

    static public final char TYPE_MESSAGE = 'M';
    static public final char TYPE_CONFIRM  = 'C';
    static public final char TYPE_REGISTER  = 'R';

    public Message(InetAddress from, int port, String message, char type) {
        this.from = from;
        this.port = port;
        this.message = message;
        this.type = type;
    }

    public InetAddress getFrom() {
        return from;
    }

    public String getMessage() {
        return message;
    }

    public int getPort() {
        return port;
    }

    public char getType() {
        return type;
    }
}
