package io.quarkus.reactivemessaging.websocket.sink.app;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.jboss.logging.Logger;

@ApplicationScoped
@ServerEndpoint("/ws-target-url")
public class WebSocketEndpoint {
    private static final Logger log = Logger.getLogger(WebSocketEndpoint.class);
    private final List<String> messages = new ArrayList<>();

    private final List<Session> sessions = new ArrayList<>();

    @OnError
    void onError(Throwable error) {
        log.error("Unexpected error in the WebSocketSinkTest", error);
    }

    @OnOpen
    void onOpen(Session session) {
        sessions.add(session);
    }

    @OnMessage
    void consumeMessage(byte[] message) {
        messages.add(new String(message));
    }

    public void killAllSessions() {
        for (Session session : sessions) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
        try {
            Thread.sleep(1000); // wait for the web socket sessions to be closed
        } catch (InterruptedException ignored) {
        }
        sessions.clear();
    }

    public List<String> getMessages() {
        return messages;
    }

    public void reset() {
        messages.clear();

        killAllSessions();
    }

    public int sessionCount() {
        return sessions.size();
    }
}
