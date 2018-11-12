package ru.nsu.ccfit;

import ru.nsu.ccfit.Containers.CheckTask;
import ru.nsu.ccfit.Containers.NodeInfo;
import ru.nsu.ccfit.Containers.Storage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CopyOnWriteArrayList;

public class Checker extends Thread {
    private Storage<CheckTask> checkQueue;
    private boolean stopping;
    private CopyOnWriteArrayList<NodeInfo> nodes;
    private final Object stopSync = new Object();
    private DatagramSocket socket;
    static private final long maxFailed = 3;//Max message amounts

    public Checker(Storage<CheckTask> checkQueue, DatagramSocket socket, CopyOnWriteArrayList<NodeInfo> nodes) {
        this.checkQueue = checkQueue;
        this.stopping = false;
        this.socket = socket;
        this.nodes = nodes;
    }

    public void stopThread() {
        synchronized (stopSync) {
            stopping = true;
        }
    }

    @Override
    public void run() {
        while (true) {
            CheckTask task = checkQueue.get();
            if (task == null) {
                break;
            }
            long sleepTime = task.getCheckTime()-System.currentTimeMillis();
            System.err.println("Will sleep for "+sleepTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.err.println("waked up");
            if (task.getNumFailed() > maxFailed) {
                nodes.remove(task.getNode());
                System.err.println("Node timeouted");
            } else if (task.getUid()>task.getNode().getLastDelivered()) {
                byte[] buf = new byte[256];
                buf = task.getMessage().getBytes();
                try {
                    socket.send(new DatagramPacket(buf,buf.length,task.getNode().getAddress(),task.getNode().getPort()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.err.println("Message resent");
                task.updateTask();
                checkQueue.put(task);
            }
            synchronized (stopSync) {
                if (stopping) {
                    break;
                }
            }
        }
    }
}
