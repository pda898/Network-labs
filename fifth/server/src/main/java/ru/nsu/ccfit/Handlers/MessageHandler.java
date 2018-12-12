package ru.nsu.ccfit.Handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.nsu.ccfit.*;

import java.io.InputStreamReader;
import java.io.Reader;

public class MessageHandler implements HttpHandler {
    private UserStorage users;
    private MessageStorage messages;
    private static Gson gson = new GsonBuilder().serializeNulls().create();

    public MessageHandler(UserStorage users, MessageStorage messages) {
        this.users = users;
        this.messages = messages;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.getRequestMethod().toString().equals("POST")) {
            newMessage(exchange);
        }else if (exchange.getRequestMethod().toString().equals("GET")) {
            listMessage(exchange);
        } else {
            CommonHandlers.wrongMethodHandler(exchange);
        }
    }

    private void newMessage(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        exchange.startBlocking();
        Reader is = new InputStreamReader(exchange.getInputStream());
        JsonObject tmp = new JsonParser().parse(is).getAsJsonObject();
        String token = CommonHandlers.getToken(exchange);
        User user = users.checkToken(token);
        if(user == null) {
            CommonHandlers.authErrorHandler(exchange);
        } else {
            try {
                Message message = messages.create(tmp.get("message").getAsString(),user.getId());
                exchange.setStatusCode(200);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"application/json");
                exchange.getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
                exchange.getResponseSender().send("{\"id\":"+message.getId()+",\"message\":\""+message.getMessage()+"\"}");
            }
            catch (NullPointerException e) {
                CommonHandlers.wrongFormatHandler(exchange);
            }
        }
    }

    private void listMessage(HttpServerExchange exchange) {
        String token = CommonHandlers.getToken(exchange);
        User user = users.checkToken(token);
        if(user == null) {
            CommonHandlers.authErrorHandler(exchange);
        } else {
            user.update();
            int offset = 0, count = 10;
            if (exchange.getQueryParameters().get("offset").size() != 0) {
                offset = Integer.valueOf(exchange.getQueryParameters().get("offset").getFirst());
                offset = Integer.max(offset,0);
            }
            if (exchange.getQueryParameters().get("count").size() != 0) {
                count = Integer.valueOf(exchange.getQueryParameters().get("count").getFirst());
                count = Integer.max(count,0);
            }
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
            exchange.getResponseSender().send("{\"messages\":"+gson.toJson(messages.getAll(offset,count))+"}");
        }
    }
}
