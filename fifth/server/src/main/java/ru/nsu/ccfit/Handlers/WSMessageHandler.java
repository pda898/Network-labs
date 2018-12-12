package ru.nsu.ccfit.Handlers;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import ru.nsu.ccfit.MessageStorage;
import ru.nsu.ccfit.UserStorage;

public class WSMessageHandler implements WebSocketConnectionCallback {
    private UserStorage users;
    private MessageStorage messages;

    public WSMessageHandler(UserStorage users, MessageStorage messages) {
        this.users = users;
        this.messages = messages;
    }
    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel webSocketChannel) {
        webSocketChannel
                .getReceiveSetter()
                .set(new Updater(users,messages));

        webSocketChannel.resumeReceives();
    }
}




