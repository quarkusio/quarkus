package io.quarkus.it.websocket;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/wsopen")
@ApplicationScoped
public class WebSocketOpenEndpoint {

    public static String[] messages = { "23.0", "1.0", "99.0", "10.0" };

    @OnOpen
    public void onOpen(Session session) throws IOException {
        for (String i : messages) {
            session.getAsyncRemote().sendText(i);
        }
    }
}
