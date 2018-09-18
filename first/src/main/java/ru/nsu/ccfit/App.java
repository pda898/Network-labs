package ru.nsu.ccfit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.*;
import java.util.TreeMap;

public class App
{

    private static final String pingMessage = "Alive";
    private static final String exitMessage = "Leave";

    private static final int port = 12345;
    private static final int timeout = 5;//in seconds

    private TreeMap<String,Long> knownPrograms = new TreeMap<>();
    private InetAddress group;

    public static void main( String[] args )
    {
        if (args.length != 1) {
            System.out.println("Cannot found address in params");
            return;
        }

        App runner  = new App();

        try {
            MulticastSocket s = runner.connect(args[0]);
            runner.work(s);
            runner.leave(s);
        } catch (IllegalArgumentException | UnknownHostException e){
            System.out.println("Wrong address");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    Main loop
     */
    public void work(MulticastSocket socket) throws IOException{
        byte[] buf = new byte[5];
        boolean timeoutReached;
        DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);
        Reader reader = new InputStreamReader(System.in);
        System.out.println("Starting search for program copies");
        timeoutReached = true;
        while (!reader.ready()) {
            if (timeoutReached) {
                DatagramPacket pingPacket = new DatagramPacket(pingMessage.getBytes(), pingMessage.length(), group, port);
                socket.send(pingPacket);
                timeoutReached = false;
            }
            try {
                socket.receive(recvPacket);
            } catch (SocketTimeoutException e) {
                timeoutReached = true;
            }
            if (!timeoutReached) {
                parseRecvPacket(recvPacket);
            }
        }
        reader.read(); //To extract exit command from buffer and avoid dumping it into shell or another text field
    }

    /*
    Create socket and setuping for multicast
     */
    public MulticastSocket connect(String address) throws IOException {
        group = InetAddress.getByName(address);
        MulticastSocket socket = new MulticastSocket(port);
        socket.setSoTimeout(timeout*1000);
        socket.setLoopbackMode(true); //So we will not recieve our own messages
        socket.joinGroup(group);
        return socket;
    }

    /*
    Send exit message and close socket
     */
    public void leave(MulticastSocket socket) throws IOException {
        DatagramPacket leavePacket = new DatagramPacket(exitMessage.getBytes(),exitMessage.length(),group,port);
        socket.send(leavePacket);
        socket.leaveGroup(group);
        socket.close();
    }

    /*
    Parsing recieved packet into map and print changes.
     */
    private void parseRecvPacket(DatagramPacket recvPacket) {
        String message = new String(recvPacket.getData());
        switch (message) {
            case pingMessage:
                knownPrograms.put(recvPacket.getAddress().getHostAddress(), System.currentTimeMillis());
                break;
            case exitMessage:
                knownPrograms.remove(recvPacket.getAddress().getHostAddress());
                break;
            default:
                System.out.println("wtf");
                return;
        }
        System.out.println("List of active programs");
        long currTime = System.currentTimeMillis();
        if (knownPrograms.size() == 0) {
            System.out.println("No copies detected");
        }
        /*
        Lambda - foreach for map, printing it while clearing entries from 5+min ago
         */
        knownPrograms.forEach((k,v) -> {if ((currTime - v) < 300*1000)
        {System.out.println(k);}
        else {knownPrograms.remove(k);}});
    }
}
