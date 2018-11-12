package ru.nsu.ccfit.Handlers;

import com.google.gson.Gson;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.nsu.ccfit.User;
import ru.nsu.ccfit.UserStorage;

public class LogoutHandler implements HttpHandler {
    private UserStorage users;

    public LogoutHandler(UserStorage users) {
        this.users = users;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String token = CommonHandlers.getToken(exchange);
        User user = users.checkToken(token);
        if(user == null) {
            CommonHandlers.authErrorHandler(exchange);
        }
        else {
            users.delete(user, token);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send("{\"message\":\"bye!\"}");
        }
    }
}
