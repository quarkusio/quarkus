package io.quarkus.websockets.test.handshake;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import io.vertx.core.Context;

/**
 * Records the thread the handshake ran on, blocks for a moment and adds a response header.
 */
public class HandshakeRecordingConfigurator extends ServerEndpointConfig.Configurator {

    public static final String HEADER = "X-Handshake-Thread";

    public static final AtomicReference<String> THREAD = new AtomicReference<>();
    public static final AtomicReference<Boolean> ON_EVENT_LOOP = new AtomicReference<>();

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        THREAD.set(Thread.currentThread().getName());
        ON_EVENT_LOOP.set(Context.isOnEventLoopThread());
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        response.getHeaders().put(HEADER, List.of(Thread.currentThread().getName()));
    }
}
