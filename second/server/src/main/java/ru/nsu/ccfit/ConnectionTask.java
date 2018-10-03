package ru.nsu.ccfit;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

public class ConnectionTask implements Runnable {
    private Socket socket;
    private DataInputStream in;
    private PrintWriter out;
    private String filename = "";
    private static int bufferSize = 4096;//in bytes
    private static int nameLength = 4096;
    static private int tickTime = 3000;//update interval
    static private int timeout = 25000;//time until client will count as timeouted

    //timer vars
    private long maxSize;
    private long lastSize;
    private long startTime;

    ConnectionTask(Socket socket) {
        this.socket = socket;
        try {
            this.socket.setSoTimeout(tickTime);
            in = new DataInputStream(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
        }catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        FileOutputStream file = null;
        Boolean requestCleanup = false;
        byte[] buffer = new byte[bufferSize];
        byte[] name = new byte[nameLength];
        try {
            in.readFully(name,0,nameLength);
            filename = parse(new String(name));
            System.out.println("uploads"+File.separator+filename);
            file = new FileOutputStream("uploads"+File.separator+filename);
            long size = in.readLong();
            long lastTime = System.currentTimeMillis();
            initTimer(size,lastTime);
            while (size > 0) {
                try {
                    int read = in.read(buffer);
                    if (read == -1) {
                        throw new IOException("Connection closed");
                    }
                    file.write(buffer, 0, read);
                    size -= read;
                    if (System.currentTimeMillis() - lastTime > tickTime) {
                        lastTime = update(size,lastTime);
                    }
                } catch (SocketTimeoutException e) {
                    update(size,lastTime);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Cannot create file "+e.getMessage()+" for client");
            requestCleanup = true;
        } catch (TimeoutException e) {
            System.out.println("File download "+filename+" was stopped (reason: timeout)");
            requestCleanup = true;
        } catch (EOFException e) {
            System.out.println("Connection was reset by peer for file "+filename);
            requestCleanup = true;
        } catch (IOException e) {
            System.out.println("I/O error: "+e.getLocalizedMessage());
            requestCleanup = true;
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (requestCleanup) {
                cleanup(filename);
            } else {
                System.out.println(filename+": transmission completed");
            }
            try {
                out.println("Success");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static private String parse(String in) throws FileNotFoundException {
        int pos = Integer.max(in.lastIndexOf("/"),in.lastIndexOf("\\"));
        pos = Integer.max(pos,0);
        String result = in.substring(pos).trim();//trim because zero bytes
        File file = new File("uploads"+File.separator+result);
        if (file.exists()) {
            throw new FileNotFoundException(result);
        }
        return result;
    }

    private void initTimer(long size, long time) {
        maxSize = size;
        startTime = time;
        lastSize = size;
    }

    private long update(long currSize, long lastTime) throws TimeoutException {
        long now = System.currentTimeMillis();
        if (now - lastTime > timeout) {
            throw new TimeoutException();
        }
        System.out.format("%s: %10.3f bytes/sec (Total: %10.3f)\n",filename,((lastSize-currSize)*1000.0/(now-lastTime)),((maxSize-currSize)*1000.0/(now-startTime)));
        lastSize = currSize;
        return now;
    }

    private void cleanup(String filename) {
        if (!filename.equals("")) {
            File file = new File("uploads" + File.separator + filename);
            file.delete();
        }
    }
}
