package ru.nsu.ccfit;

import ru.nsu.ccfit.Containers.Message;
import ru.nsu.ccfit.Containers.NodeInfo;
import ru.nsu.ccfit.Containers.Storage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Reciever extends Thread {
    private Storage<Message> messageQueue;
    private CopyOnWriteArrayList<NodeInfo> nodes;
    private DatagramSocket socket;
    private String name;
    private int failPercent;
    private boolean stopping;
    private final Object stopSync = new Object();

    public Reciever(Storage<Message> messageQueue, CopyOnWriteArrayList<NodeInfo> nodes, DatagramSocket socket, String name, int failPercent) {
        this.name = name;
        this.messageQueue = messageQueue;
        this.nodes = nodes;
        this.socket = socket;
        this.failPercent = failPercent;
        this.stopping = false;
    }

    @Override
    public void run() {
        while (true) {
            try {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String text = new String(packet.getData());
                text = text.substring(0,text.indexOf(0));
                NodeInfo node = findNode(packet.getAddress(),packet.getPort());
                switch (text.charAt(0)){
                    case Message.TYPE_MESSAGE:
                        if (node == null) {
                            break;
                        }
                        text = text.substring(1);
                        if (ThreadLocalRandom.current().nextInt(0, 100 + 1) > failPercent) {
                            if (node.expectedID() == Integer.parseInt(text.substring(0, text.indexOf(":")))) {
                                System.out.println(node.getNodeName() + ": " + text.substring(text.indexOf(":") + 1));
                                messageQueue.put(new Message(packet.getAddress(), packet.getPort(), text.substring(text.indexOf(":") + 1), Message.TYPE_MESSAGE));
                                text = String.valueOf(Message.TYPE_CONFIRM);
                                buf = text.getBytes();
                                packet = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
                                socket.send(packet);
                                node.updateRecv();
                            }
                        }
                        break;
                    case Message.TYPE_CONFIRM:
                        if (node == null) {
                            break;
                        }
                        node.updateLastDelivered();
                        System.err.println("Message confirmed: uid "+node.getLastDelivered()+" for node "+node.getNodeName());
                        break;
                    case Message.TYPE_REGISTER:
                        NodeInfo newNode = new NodeInfo(packet.getAddress(),packet.getPort(),text.substring(1));
                        nodes.add(newNode);
                        text = name;
                        buf = text.getBytes();
                        packet = new DatagramPacket(buf,buf.length,newNode.getAddress(),newNode.getPort());
                        socket.send(packet);
                }
            } catch (SocketTimeoutException e) {
                //0 action required
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (stopSync) {
                if (stopping) {
                    break;
                }
            }
        }
    }

    public void stopThread() {
        synchronized (stopSync) {
            stopping = true;
        }
    }

    private NodeInfo findNode(InetAddress address, int port) {
        for (NodeInfo node: nodes) {
            if (node.getAddress().equals(address) && (node.getPort() == port)) {
                return node;
            }
        }
        return null;
    }

}
