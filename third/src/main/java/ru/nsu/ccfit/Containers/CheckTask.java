package ru.nsu.ccfit.Containers;

public class CheckTask {
    private long uid;
    private NodeInfo node;
    private String message;
    private int numFailed = 1;
    private long checkTime;
    static private long comfirmTimeout = 5000L;

    public CheckTask(long uid, NodeInfo node, String message) {
        this.uid = uid;
        this.node = node;
        this.message = message;
        checkTime = System.currentTimeMillis()+comfirmTimeout;
    }

    public long getUid() {
        return uid;
    }

    public NodeInfo getNode() {
        return node;
    }

    public String getMessage() {
        return message;
    }

    public int getNumFailed() {
        return numFailed;
    }

    public long getCheckTime() {
        return checkTime;
    }

    public void updateTask() {
        numFailed++;
        this.checkTime = System.currentTimeMillis()+comfirmTimeout;
    }
}
