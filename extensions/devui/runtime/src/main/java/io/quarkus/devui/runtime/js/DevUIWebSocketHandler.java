package io.quarkus.devui.runtime.js;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.devui.runtime.comms.JsonRpcRouter;
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
            event.request().toWebSocket(new Handler<AsyncResult<ServerWebSocket>>() {
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

    private void addSocket(ServerWebSocket session) {
        try {
            JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
            jsonRpcRouter.addSocket(session);
        } catch (IllegalStateException ise) {
            LOG.debug("Failed to connect to dev ui ws server, " + ise.getMessage());
        }
    }

    private static final String UPGRADE = "Upgrade";
    private static final String WEBSOCKET = "websocket";
}
