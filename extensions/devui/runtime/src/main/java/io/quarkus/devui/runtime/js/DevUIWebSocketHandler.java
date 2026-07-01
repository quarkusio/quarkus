package io.quarkus.devui.runtime.js;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

/**
 * This is the main entry point for Dev UI Json RPC communication
 */
public class DevUIWebSocketHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(DevUIWebSocketHandler.class.getName());

    @Override
    public void handle(RoutingContext event) {
        if (WEBSOCKET.equalsIgnoreCase(event.request().getHeader(UPGRADE)) && !event.request().isEnded()) {
            event.request().toWebSocket().onComplete(new Handler<AsyncResult<ServerWebSocket>>() {
                @Override
                public void handle(AsyncResult<ServerWebSocket> event) {
                    if (event.succeeded()) {
                        ServerWebSocket socket = event.result();
                        addSocket(socket);
                    } else {
                        LOG.debug("Failed to connect to dev ui ws server", event.cause());
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
            DevUISessionManager sessionManager = CDI.current().select(DevUISessionManager.class).get();
            JavaScriptResponseWriter writer = new JavaScriptResponseWriter(socket);
            sessionManager.addSession(writer);
            socket.textMessageHandler((e) -> {
                JsonRpcRequest jsonRpcRequest = jsonRpcRouter.getJsonRpcCodec().readRequest(e);
                jsonRpcRouter.route(jsonRpcRequest, writer);
            }).closeHandler((e) -> {
                sessionManager.purge();
            });
        } catch (IllegalStateException ise) {
            LOG.debug("Failed to connect to dev ui ws server, " + ise.getMessage());
        }
    }

    private static final String UPGRADE = "Upgrade";
    private static final String WEBSOCKET = "websocket";
}
