package ru.nsu.ccfit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.websocket.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

@ClientEndpoint
public class WSConnection {
    private Gson g = new Gson();
    private Type messageListType = new TypeToken<ArrayList<Message>>(){}.getType();
    private static ArrayList<Message> messages = new ArrayList<>();
    public static String token;
    final private static Object sync = new Object();
    @OnOpen
    public void onOpen(Session session) {
        try {
            session.getBasicRemote().sendText(token);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    @OnMessage
    public void processMessage(String message) {
        if (message.equals("Connected")) {
            return;
        }
        ArrayList<Message> newMessages;
        String body = message;
        body = body.substring(body.indexOf('['), body.length() - 1);
        newMessages = g.fromJson(body, messageListType);
        synchronized (sync) {
            messages.addAll(newMessages);
        }

    }

    public static ArrayList<Message> getMessages() {
        ArrayList<Message> copy = new ArrayList<>();
        synchronized (sync) {
            copy.addAll(messages);
            messages.clear();
        }
        return copy;
    }

    @OnError
    public void processError(Throwable t) {
        t.printStackTrace();
    }
}
