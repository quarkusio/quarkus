package ilove.quark.us;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

@ServerEndpoint("/start-websocket/{name}")
@ApplicationScoped
public class StartWebSocket {

    @OnOpen
    public void onOpen(Session session, @PathParam("name") String name) {
        System.out.println("onOpen> " + name);
    }

    @OnClose
    public void onClose(Session session, @PathParam("name") String name) {
        System.out.println("onClose> " + name);
    }

    @OnError
    public void onError(Session session, @PathParam("name") String name, Throwable throwable) {
        System.out.println("onError> " + name + ": " + throwable);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String name) {
        System.out.println("onMessage> " + name + ": " + message);
    }

    /**
     * Send a message to a remote websocket session
     *
     * @param session the websocket session
     * @param message the message to send
     * @throws IOException              if there is a communication error sending the message object.
     * @throws EncodeException          if there was a problem encoding the message.
     * @throws IllegalArgumentException if the message parameter is {@code null}
     */
    public void sendMessage(Session session, String message) throws EncodeException, IOException {
        requireNonNull(session, "session is required");

        session.getBasicRemote().sendObject(message);
    }

}
