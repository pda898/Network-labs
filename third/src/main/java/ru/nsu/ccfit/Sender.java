package ru.nsu.ccfit;

import ru.nsu.ccfit.Containers.CheckTask;
import ru.nsu.ccfit.Containers.Message;
import ru.nsu.ccfit.Containers.NodeInfo;
import ru.nsu.ccfit.Containers.Storage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CopyOnWriteArrayList;

public class Sender extends Thread{
    private Storage<Message> messageQueue;
    private CopyOnWriteArrayList<NodeInfo> nodes;
    private Storage<CheckTask> checkQueue = new Storage<>();
    private DatagramSocket socket;
    private Checker checker;
    private boolean stopping;
    private final Object stopSync = new Object();

    public Sender(Storage<Message> messageQueue, CopyOnWriteArrayList<NodeInfo> nodes, DatagramSocket socket) {
        this.messageQueue = messageQueue;
        this.nodes = nodes;
        this.socket = socket;
        this.stopping = false;
        this.checker = new Checker(checkQueue, socket, nodes);
        checker.start();
    }

    public void stopThread() {
        synchronized (stopSync) {
            stopping = true;
        }
    }

    @Override
    public void run() {
        while (true) {
            Message message = messageQueue.get();
            if (message == null) {
                checker.stopThread();
                checkQueue.wakeup();
                try {
                    checker.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            try {
                String text;
                for(NodeInfo client: nodes) {
                    if (!client.getAddress().equals(message.getFrom()) || (client.getPort() != message.getPort())) {
                        byte[] buf = new byte[256];
                        long uid = client.getSendID();
                        text = String.valueOf(message.getType())+Long.toString(uid)+":"+message.getMessage();
                        buf = text.getBytes();
                        socket.send(new DatagramPacket(buf,buf.length,client.getAddress(),client.getPort()));
                        System.err.println("Message sent");
                        checkQueue.put(new CheckTask(uid,client,text));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (stopSync) {
                if (stopping) {
                    checker.stopThread();
                    checkQueue.wakeup();
                    try {
                        checker.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
}
