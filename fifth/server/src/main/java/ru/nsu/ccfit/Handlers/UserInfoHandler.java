package ru.nsu.ccfit.Handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.nsu.ccfit.User;
import ru.nsu.ccfit.UserStorage;

public class UserInfoHandler implements HttpHandler {
    private UserStorage users;
    private static Gson gson = new GsonBuilder().serializeNulls().create();

    public UserInfoHandler(UserStorage users) {
        this.users = users;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (!exchange.getRequestMethod().toString().equals("GET")) {
            CommonHandlers.wrongMethodHandler(exchange);
            return;
        }
        String token = CommonHandlers.getToken(exchange);
        User user = users.checkToken(token);
        if(user == null) {
            CommonHandlers.authErrorHandler(exchange);
        } else {
            user.update();
            User target = users.get(Long.valueOf(exchange.getQueryParameters().get("id").getFirst()));
            if (target == null) {
                CommonHandlers.notFoundHandler(exchange);
            }
            else {
                exchange.setStatusCode(200);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exchange.getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
                exchange.getResponseSender().send(gson.toJson(target));
            }
        }
    }
}
