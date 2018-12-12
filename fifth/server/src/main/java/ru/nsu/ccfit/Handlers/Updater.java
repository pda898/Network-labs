package ru.nsu.ccfit.Handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import ru.nsu.ccfit.MessageStorage;
import ru.nsu.ccfit.User;
import ru.nsu.ccfit.UserStorage;

import java.io.IOException;

public class Updater extends AbstractReceiveListener {
    private MessageStorage messages;
    private UserStorage users;
    private static Gson gson = new GsonBuilder().serializeNulls().create();

    public Updater(UserStorage users, MessageStorage messages) {
        this.users = users;
        this.messages = messages;
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
        User user = users.checkToken(message.getData());
        if(user == null) {
            WebSockets.sendText("Token realm='Username is already in use'",channel,null);
            return;
        }
        WebSockets.sendText("Connected",channel,null);
        int n = messages.size();
        WebSockets.sendText("{\"messages\":"+gson.toJson(messages.getAll(0,-1))+"}", channel, null);
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int currSize = messages.size();
            if (n < currSize) {
                System.out.println(n);
                System.out.println(currSize);
                WebSockets.sendText("{\"messages\":" + gson.toJson(messages.getAll(n,currSize-n)) + "}", channel, null);
                n = currSize;
            }
        }
    }
}