package ru.nsu.ccfit;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server
{
    private ServerSocket socket;
    private ThreadPoolExecutor threads;

    static private int timeout = 5000;//interval when program will scan for user interrupt
    static private int numThreads = 10;//now many clients can be connected at the same time

    public Server(int port) throws IOException {
        socket = new ServerSocket(port);
        socket.setSoTimeout(timeout);
        threads = new ThreadPoolExecutor(1, numThreads, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(numThreads));
        File dir = new File("uploads");
        dir.mkdir();
    }

    public static void main(String[] args ) {
        if (args.length == 0) {
            System.out.println("Cannot find port");
            return;
        }
        try {
            System.out.println("test");
            Server server = new Server(Integer.parseInt(args[0]));
            System.out.println("Launching server");
            server.work();
        } catch (IllegalArgumentException e) {
            System.err.println("Wrong socket number");
        } catch (IOException e) {
            System.err.println("I/O error");
        }
    }

    //Main working loop
    public void work() throws IOException {
        Reader reader = new InputStreamReader(System.in);
        Socket newSocket = null;
        while (!reader.ready()) {
            try {
                newSocket = socket.accept();
                threads.execute(new ConnectionTask(newSocket));
            } catch (SocketTimeoutException e) {
                //should be empty because 0 reaction requires in that case
            } catch (RejectedExecutionException e) {
                if (newSocket != null) {
                    newSocket.close();
                }
            }
        }
        reader.read(); //To extract exit command from buffer and avoid dumping it into shell or another text field
        this.close();
    }

    //Close socket and kill all downloads
    private void close() {
        try {
            socket.close();
            threads.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
