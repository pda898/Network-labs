package ru.nsu.ccfit.Handlers;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class CommonHandlers {
    public static void wrongFormatHandler(HttpServerExchange exchange) {
        exchange.setStatusCode(400);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"application/json");
        exchange.getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
    }

    public static void authErrorHandler(HttpServerExchange exchange) {
        exchange.setStatusCode(401);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"application/json");
        exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, "Token realm='Username is already in use'");
    }

    public static void notFoundHandler(HttpServerExchange exchange) {
        exchange.setStatusCode(404);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"application/json");
        exchange.getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
    }

    public static void wrongMethodHandler(HttpServerExchange exchange) {
        exchange.setStatusCode(405);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,"application/json");
        exchange.getResponseHeaders().remove(Headers.WWW_AUTHENTICATE);
    }

    public static String getToken(HttpServerExchange exchange) {
        if (!exchange.getResponseHeaders().contains(Headers.AUTHORIZATION)) {
            CommonHandlers.authErrorHandler(exchange);
        }
        return exchange.getRequestHeaders().get(Headers.AUTHORIZATION).getFirst().substring(6);
    }
}
