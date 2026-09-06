package io.quarkus.produi.runtime;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.devjsonrpc.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.comms.MessageType;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

/**
 * WebSocket handler for Prod UI JsonRPC communication.
 * Handles WebSocket upgrade on the management router and routes
 * JsonRPC requests through the Prod UI router.
 */
public class ProdUIWebSocketHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(ProdUIWebSocketHandler.class);
    private static final String UPGRADE = "Upgrade";
    private static final String WEBSOCKET = "websocket";

    @Override
    public void handle(RoutingContext event) {
        if (WEBSOCKET.equalsIgnoreCase(event.request().getHeader(UPGRADE)) && !event.request().isEnded()) {
            event.request().toWebSocket().onComplete(new Handler<AsyncResult<ServerWebSocket>>() {
                @Override
                public void handle(AsyncResult<ServerWebSocket> event) {
                    if (event.succeeded()) {
                        addSocket(event.result());
                    } else {
                        LOG.debug("Failed to connect to prod-ui ws server", event.cause());
                    }
                }
            });
            return;
        }
        event.next();
    }

    private void addSocket(ServerWebSocket socket) {
        try {
            JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
            ProdUIResponseWriter writer = new ProdUIResponseWriter(socket);
            socket.textMessageHandler((message) -> {
                JsonRpcRequest jsonRpcRequest = jsonRpcRouter.getJsonRpcCodec().readRequest(message);
                jsonRpcRouter.route(jsonRpcRequest, writer);
            }).closeHandler((e) -> {
                // no session tracking needed for prod
            });
        } catch (IllegalStateException ise) {
            LOG.debug("Failed to connect to prod-ui ws server, " + ise.getMessage());
        }
    }

    static class ProdUIResponseWriter implements JsonRpcResponseWriter {
        private final ServerWebSocket socket;

        ProdUIResponseWriter(ServerWebSocket socket) {
            this.socket = socket;
        }

        @Override
        public void write(String message) {
            if (!socket.isClosed()) {
                socket.writeTextMessage(message);
            }
        }

        @Override
        public void close() {
            socket.close();
        }

        @Override
        public boolean isOpen() {
            return !socket.isClosed();
        }

        @Override
        public boolean isClosed() {
            return socket.isClosed();
        }

        @Override
        public Object decorateObject(Object object, MessageType messageType) {
            return new Result(messageType.name(), object);
        }
    }
}
