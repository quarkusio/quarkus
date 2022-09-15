package io.quarkus.it.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

@ClientEndpoint(decoders = ClientDtoDecoder.class, encoders = ClientDtoEncoder.class)
public class CodingClient {
    private static List<Session> sessions = Collections.synchronizedList(new ArrayList<>());

    static LinkedBlockingDeque<Dto> messageQueue = new LinkedBlockingDeque<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);

        Dto data = new Dto();
        data.setContent("initial data");
        session.getAsyncRemote().sendObject(data);
    }

    @OnMessage
    public void onMessage(Dto message) {
        messageQueue.add(message);
        close();
    }

    static void close() {
        for (Session session : sessions) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }

    }
}
