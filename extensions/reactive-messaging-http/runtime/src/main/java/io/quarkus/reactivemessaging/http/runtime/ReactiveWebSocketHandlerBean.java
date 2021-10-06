package io.quarkus.reactivemessaging.http.runtime;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.config.ReactiveHttpConfig;
import io.quarkus.reactivemessaging.http.runtime.config.WebSocketStreamConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

/**
 * a bean that handles incoming web socket messages
 */
@Singleton
public class ReactiveWebSocketHandlerBean extends ReactiveHandlerBeanBase<WebSocketStreamConfig, WebSocketMessage<?>> {

    private static final Logger log = Logger.getLogger(ReactiveWebSocketHandlerBean.class);

    @Inject
    ReactiveHttpConfig config;

    @Override
    protected void handleRequest(RoutingContext event, MultiEmitter<? super WebSocketMessage<?>> emitter,
            StrictQueueSizeGuard guard, String path) {
        event.request().toWebSocket(
                webSocket -> {
                    if (webSocket.failed()) {
                        log.error("failed to connect web socket", webSocket.cause());
                    } else {
                        ServerWebSocket serverWebSocket = webSocket.result();
                        serverWebSocket.handler(
                                b -> {
                                    if (emitter == null) {
                                        onUnexpectedError(serverWebSocket, null,
                                                "No consumer subscribed for messages sent to " +
                                                        "Reactive Messaging WebSocket endpoint on path: " + path);
                                    } else if (guard.prepareToEmit()) {
                                        try {
                                            emitter.emit(new WebSocketMessage<>(b,
                                                    () -> serverWebSocket.write(Buffer.buffer("ACK")),
                                                    error -> onUnexpectedError(serverWebSocket, error,
                                                            "Failed to process incoming web socket message.")));
                                        } catch (Exception error) {
                                            guard.dequeue();
                                            onUnexpectedError(serverWebSocket, error, "Emitting message failed");
                                        }
                                    } else {
                                        serverWebSocket.write(Buffer.buffer("BUFFER_OVERFLOW"));
                                    }
                                });
                    }
                });
    }

    @Override
    protected String description(WebSocketStreamConfig config) {
        return String.format("path %s", config.path);
    }

    @Override
    protected String key(WebSocketStreamConfig config) {
        return config.path;
    }

    @Override
    protected String key(RoutingContext context) {
        return context.normalizedPath();
    }

    @Override
    protected Collection<WebSocketStreamConfig> configs() {
        return config.getWebSocketConfigs();
    }

    private void onUnexpectedError(ServerWebSocket serverWebSocket, Throwable error, String message) {
        log.error(message, error);
        // TODO some error message for the client? exception mapper would be best...
        serverWebSocket.close((short) 3500, "Unexpected error while processing the message");
    }

    Multi<WebSocketMessage<?>> getProcessor(String path) {
        Bundle<WebSocketMessage<?>> bundle = processors.get(path);
        if (bundle == null) {
            throw new IllegalStateException("No incoming stream defined for path " + path);
        }
        return bundle.getProcessor();
    }
}
