package ru.nsu.ccfit.Handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.nsu.ccfit.LoginInfo;
import ru.nsu.ccfit.UserStorage;

import java.io.InputStreamReader;
import java.io.Reader;

public class LoginHandler implements HttpHandler {
    private UserStorage users;
    private static Gson gson = new GsonBuilder().serializeNulls().create();

    public LoginHandler(UserStorage users) {
        this.users = users;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        exchange.startBlocking();
        Reader is = new InputStreamReader(exchange.getInputStream());
        JsonObject tmp = new JsonParser().parse(is).getAsJsonObject();
        try {
            LoginInfo newUser = users.create(tmp.get("username").getAsString());
            if (newUser == null) {
                CommonHandlers.authErrorHandler(exchange);
            }
            else {
                exchange.setStatusCode(200);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"application/json");
                exchange.getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
                exchange.getResponseSender().send(gson.toJson(newUser));
            }
        }
        catch (NullPointerException e) {
            CommonHandlers.wrongFormatHandler(exchange);
        }
    }
}
