package ru.nsu.ccfit;

import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import ru.nsu.ccfit.Handlers.*;

/**
 * Hello world!
 *
 */
public class Server
{
    public static void main( String[] args )
    {
        String host = "localhost";
        int port = 8080;
        if (args.length == 2) {
            host = args[0];
            port = Integer.valueOf(args[1]);
        }
        UserStorage users = new UserStorage();
        MessageStorage messages = new MessageStorage();
        RoutingHandler routes = new RoutingHandler().post("/login",new LoginHandler(users))
                .post("/logout",new LogoutHandler(users))
                .get("/users", new UserListHandler(users))
                .get("/users/{id}", new UserInfoHandler(users))
                .post("/messages",new MessageHandler(users,messages))
                .get("/messages",new MessageListHandler(users,messages))
                .setFallbackHandler(CommonHandlers::notFoundHandler);
        Undertow server = Undertow.builder().addHttpListener(port,host).setHandler(routes).build();
        server.start();
    }
}
