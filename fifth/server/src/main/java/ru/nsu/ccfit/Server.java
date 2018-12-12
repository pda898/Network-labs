package ru.nsu.ccfit;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import ru.nsu.ccfit.Handlers.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Handler;

public class Server
{
    private static UserStorage users = new UserStorage();
    private static MessageStorage messages = new MessageStorage();
    private static PathHandler loadSettings(String path) {
        PathHandler routes = Handlers.path(CommonHandlers::notFoundHandler);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            String route = reader.readLine();
            while (route != null) {
                String[] parsedRoute = route.split("\\s+");
                if (parsedRoute.length != 3) {
                    throw new IllegalArgumentException();
                }
                switch (parsedRoute[2]) {
                    case "ws":
                        routes.addExactPath(parsedRoute[0], Handlers.websocket((WebSocketConnectionCallback) Class.forName(parsedRoute[1]).getDeclaredConstructor(users.getClass(),messages.getClass()).newInstance(users,messages)));break;
                    case "u":
                        routes.addPrefixPath(parsedRoute[0], (HttpHandler) Class.forName(parsedRoute[1]).getDeclaredConstructor(users.getClass()).newInstance(users));break;
                    case "m":
                        routes.addPrefixPath(parsedRoute[0], (HttpHandler) Class.forName(parsedRoute[1]).getDeclaredConstructor(messages.getClass()).newInstance(messages));break;
                    case "um":
                        routes.addPrefixPath(parsedRoute[0], (HttpHandler) Class.forName(parsedRoute[1]).getDeclaredConstructor(users.getClass(),messages.getClass()).newInstance(users,messages));break;
                    case "n":default:
                        routes.addPrefixPath(parsedRoute[0], (HttpHandler) Class.forName(parsedRoute[1]).getDeclaredConstructor().newInstance());
                }
                route = reader.readLine();
            }
        } catch (IOException | ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return routes;
    }

    public static void main( String[] args )
    {
        String host = "localhost";
        int port = 8080;
        if (args.length == 0) {
            System.out.println("Usage [request file] <server address> <server port>");
            System.exit(0);
        }
        if (args.length == 3) {
            host = args[1];
            port = Integer.valueOf(args[2]);
        }
        Undertow server = Undertow.builder().addHttpListener(port,host).setHandler(loadSettings(args[0])).build();
        server.start();
    }
}
