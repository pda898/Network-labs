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
    /*
            .post("/logout")
            .get("/users")
            .get("/users/{id}")
            .get("/messages")
            .post("/messages")*/

    public static void main( String[] args )
    {
        UserStorage users = new UserStorage();
        MessageStorage messages = new MessageStorage();
        RoutingHandler routes = new RoutingHandler().post("/login",new LoginHandler(users))
                .post("/logout",new LogoutHandler(users))
                .get("/users", new UserListHandler(users))
                .get("/users/{id}", new UserInfoHandler(users))
                .post("/messages",new MessageHandler(users,messages))
                .get("/messages",new MessageListHandler(users,messages))
                .setFallbackHandler(CommonHandlers::notFoundHandler);
        Undertow server = Undertow.builder().addHttpListener(8080,"localhost").setHandler(routes).build();
        server.start();
    }
}
