package ru.nsu.ccfit.Handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.nsu.ccfit.MessageStorage;
import ru.nsu.ccfit.User;
import ru.nsu.ccfit.UserStorage;

import java.io.InputStreamReader;
import java.io.Reader;

public class MessageListHandler implements HttpHandler {
    private UserStorage users;
    private MessageStorage messages;
    private static Gson gson = new GsonBuilder().serializeNulls().create();

    public MessageListHandler(UserStorage users, MessageStorage messages) {
        this.users = users;
        this.messages = messages;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String token = CommonHandlers.getToken(exchange);
        User user = users.checkToken(token);
        if(user == null) {
            CommonHandlers.authErrorHandler(exchange);
        } else {
            user.update();
            int offset = 0, count = 10;
            if (exchange.getQueryParameters().get("offset").size() != 0) {
                offset = Integer.valueOf(exchange.getQueryParameters().get("offset").getFirst());
            }
            if (exchange.getQueryParameters().get("count").size() != 0) {
                count = Integer.valueOf(exchange.getQueryParameters().get("count").getFirst());
            }
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
            exchange.getResponseSender().send("{\"messages\":"+gson.toJson(messages.getAll(offset,count))+"}");
        }
    }
}
