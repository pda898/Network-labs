package ru.nsu.ccfit.Containers;

import java.net.InetAddress;

public class NodeInfo {
    private InetAddress address;
    private int port;
    private String nodeName;
    private long sent;
    private long recieved;
    private long lastDelivered;

    public NodeInfo(InetAddress address, int port, String nodeName) {
        this.address = address;
        this.port = port;
        this.nodeName = nodeName;
        this.sent = 0;
        this.recieved = 1;
        this.lastDelivered = 0;
    }

    public NodeInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.nodeName = null;
        this.sent = 0;
        this.recieved = 1;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public int getPort() {
        return port;
    }

    public long getSendID() {
        sent++;
        return sent;
    }

    public long expectedID() {
        return recieved;
    }

    public void updateRecv() {
        recieved++;
    }

    public long getLastDelivered() {
        return lastDelivered;
    }

    public void updateLastDelivered() {
        lastDelivered++;
    }
}
