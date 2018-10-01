package ru.nsu.ccfit;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client
{
    public static void main( String[] args )
    {
        if (args.length != 3) {
            System.out.println("Required address, port and filename");
            System.exit(0);
        }
        FileInputStream file = null;
        Socket socket = null;
        try {
            System.out.println("Connecting to server");
            socket = new Socket(args[0],Integer.parseInt(args[1]));
            file = new FileInputStream(args[2]);
            long filesize = Files.size(Paths.get(args[2]));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            byte[] buffer = new byte[4096];
            byte[] string = args[2].getBytes();
            System.arraycopy(string,0,buffer,0,string.length);
            out.write(buffer);
            System.out.println("Sent filename");
            out.writeLong(filesize);
            System.out.println("Sent filesize");
            int readed = file.read(buffer);
            while (readed != -1) {
                out.write(buffer);
                readed = file.read(buffer);
            }
            System.out.println(in.readLine());
        } catch (FileNotFoundException e) {
            System.err.println("Cannot access file");
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection");
            System.exit(1);
        }
        finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
