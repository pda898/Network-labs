package ru.nsu.ccfit;

import ru.nsu.ccfit.Containers.Message;
import ru.nsu.ccfit.Containers.NodeInfo;
import ru.nsu.ccfit.Containers.Storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private Storage<Message> messageQueue = new Storage<>();
    private CopyOnWriteArrayList<NodeInfo> nodes = new CopyOnWriteArrayList<>();
    private DatagramSocket socket;
    private String name;
    private Sender sender;
    private Reciever reciever;
    static private int socketTimeout = 1000;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Not enough arguments");
            return;
        }
        Server server = new Server(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        if (args.length == 5) {
            try {
                if (!server.connect(InetAddress.getByName(args[3]), Integer.parseInt(args[4]))) {
                    return;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        server.work();
    }


    public Server (String name, int failPercent, int port) {
        this.name = name;
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(socketTimeout);
            sender = new Sender(messageQueue,nodes,socket);
            reciever = new Reciever(messageQueue,nodes,socket,name,failPercent);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public boolean connect(InetAddress address, int port) {
        try {
            NodeInfo newNode = new NodeInfo(address,port);
            String text = String.valueOf(Message.TYPE_REGISTER)+ name;
            byte[] buf = text.getBytes();
            DatagramPacket packet = new DatagramPacket(buf,buf.length,newNode.getAddress(),newNode.getPort());
            socket.send(packet);
            buf = new byte[256];
            packet = new DatagramPacket(buf,buf.length);
            socket.receive(packet);
            newNode.setNodeName(new String(packet.getData()));
            nodes.add(newNode);
        } catch (SocketTimeoutException e) {
            System.out.println("Cannot register, closing now");
            cleanup();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void work() {
        sender.start();
        reciever.start();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Ready to work");
            String message = reader.readLine();
            while (message != null) {
                messageQueue.put(new Message(null,0,message,Message.TYPE_MESSAGE));
                message = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping now");
        cleanup();
    }

    private void cleanup() {
        sender.stopThread();
        reciever.stopThread();
        messageQueue.wakeup();
        try {
            sender.join();
            reciever.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        socket.close();
    }
}
