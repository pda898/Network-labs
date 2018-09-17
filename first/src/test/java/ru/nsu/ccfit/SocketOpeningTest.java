package ru.nsu.ccfit;


import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class SocketOpeningTest
{
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{"224.1.3.4"},{"ff12::feed:a:dead:beef"}});
    }

    @Parameter
    public String address;

    @Test
    public void checkConnection() {
        App runner = new App();
        try {
            MulticastSocket s = runner.connect(address);
            assertTrue(s.isBound());
            s.leaveGroup(InetAddress.getByName(address));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checkClosing() {
        App runner = new App();
        try {
            MulticastSocket s = runner.connect(address);
            runner.leave(s);
            assertTrue(s.isClosed());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
