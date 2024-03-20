package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.Context;

@WebSocket(path = "/lo-multi-produce/{client}")
public class LoMultiProduce {

    @Inject
    WebSocketConnection connection;

    @Inject
    BroadcastProcessor<String> broadcast;

    @OnOpen(broadcast = true)
    Multi<String> open() {
        assertTrue(Context.isOnEventLoopThread());
        return broadcast;
    }

    @OnTextMessage
    void trigger(String message) {
        broadcast.onNext(connection.pathParam("client").toLowerCase());
    }

    @Produces
    @SessionScoped
    BroadcastProcessor<String> multiProducer() {
        return BroadcastProcessor.create();
    }

}
