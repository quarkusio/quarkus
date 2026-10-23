package io.quarkus.devui.runtime.js;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
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
        // Nothing from CDI is kept for the lifetime of the socket. Dev mode closes the open connections when a restart
        // starts and the client reconnects right away, so a socket can be opened against an application that is
        // shutting down. A client proxy captured then would resolve against that application's destroyed container,
        // and silently create a new, uninitialized router there. The router is looked up for each message instead.
        JavaScriptResponseWriter writer = new JavaScriptResponseWriter(socket);
        DevUISessionManager.addSession(writer);
        socket.textMessageHandler((e) -> {
            JsonRpcRouter jsonRpcRouter = currentRouter();
            if (jsonRpcRouter == null) {
                // A restart is in progress. Closing makes the client reconnect, and the new application only serves
                // this handler once its router is initialized.
                LOG.debug("Dev UI JSON-RPC router not available, closing the websocket so the client reconnects");
                socket.close();
                return;
            }
            try {
                JsonRpcRequest jsonRpcRequest = jsonRpcRouter.getJsonRpcCodec().readRequest(e);
                jsonRpcRouter.route(jsonRpcRequest, writer);
            } catch (RuntimeException ex) {
                if (currentRouter() != null) {
                    throw ex;
                }
                // The application stopped while the message was being dispatched, so reconnect to the new one
                LOG.debug("Dev UI JSON-RPC router stopped while dispatching a message, closing the websocket", ex);
                socket.close();
            }
        }).closeHandler((e) -> {
            DevUISessionManager.purge();
        });
    }

    /**
     * @return the router of the running application, or null if there is none or it is not initialized yet
     */
    private static JsonRpcRouter currentRouter() {
        ArcContainer container = Arc.container();
        if (container == null || !container.isRunning()) {
            return null;
        }
        JsonRpcRouter jsonRpcRouter = container.instance(JsonRpcRouter.class).get();
        return jsonRpcRouter != null && jsonRpcRouter.isReady() ? jsonRpcRouter : null;
    }

    private static final String UPGRADE = "Upgrade";
    private static final String WEBSOCKET = "websocket";
}
