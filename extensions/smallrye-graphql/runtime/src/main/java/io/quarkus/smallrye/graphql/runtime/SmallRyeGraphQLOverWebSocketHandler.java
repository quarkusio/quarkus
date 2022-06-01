package io.quarkus.smallrye.graphql.runtime;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.smallrye.graphql.websocket.GraphQLWebSocketSession;
import io.smallrye.graphql.websocket.GraphQLWebsocketHandler;
import io.smallrye.graphql.websocket.graphqltransportws.GraphQLTransportWSSubprotocolHandler;
import io.smallrye.graphql.websocket.graphqlws.GraphQLWSSubprotocolHandler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that does the execution of GraphQL Requests
 */
public class SmallRyeGraphQLOverWebSocketHandler extends SmallRyeGraphQLAbstractHandler {
    private static final Logger log = Logger.getLogger(SmallRyeGraphQLOverWebSocketHandler.class);

    public SmallRyeGraphQLOverWebSocketHandler(CurrentIdentityAssociation currentIdentityAssociation,
            CurrentVertxRequest currentVertxRequest, boolean runBlocking) {
        super(currentIdentityAssociation, currentVertxRequest, runBlocking);
    }

    @Override
    protected void doHandle(final RoutingContext ctx) {
        if (ctx.request().headers().contains(HttpHeaders.UPGRADE, HttpHeaders.WEBSOCKET, true) && !ctx.request().isEnded()) {
            ctx.request().toWebSocket(event -> {
                if (event.succeeded()) {
                    ServerWebSocket serverWebSocket = event.result();
                    String subprotocol = serverWebSocket.subProtocol();
                    GraphQLWebsocketHandler handler = null;
                    switch (subprotocol) {
                        case "graphql-transport-ws":
                            handler = new GraphQLTransportWSSubprotocolHandler(
                                    new QuarkusVertxWebSocketSession(serverWebSocket), getMetaData(ctx));
                            break;
                        case "graphql-ws":
                            handler = new GraphQLWSSubprotocolHandler(
                                    new QuarkusVertxWebSocketSession(serverWebSocket), getMetaData(ctx));
                            break;
                        default:
                            log.warn("Unknown graphql-over-websocket protocol: " + subprotocol);
                            serverWebSocket.close();
                            return;
                    }
                    log.debug("Starting websocket with subprotocol = " + subprotocol);
                    GraphQLWebsocketHandler finalHandler = handler;
                    serverWebSocket.closeHandler(v -> finalHandler.onClose());
                    serverWebSocket.endHandler(v -> finalHandler.onEnd());
                    serverWebSocket.exceptionHandler(t -> finalHandler.onThrowable(t));
                    serverWebSocket.textMessageHandler(m -> finalHandler.onMessage(m));
                } else {
                    log.warn("WebSocket failed", event.cause());
                }
            });
        } else {
            ctx.next();
        }
    }

    private static class QuarkusVertxWebSocketSession implements GraphQLWebSocketSession {

        private final ServerWebSocket webSocket;
        private final String peer;

        QuarkusVertxWebSocketSession(ServerWebSocket webSocket) {
            this.webSocket = webSocket;
            if (webSocket.remoteAddress() != null) {
                this.peer = webSocket.remoteAddress().host() + ":" + webSocket.remoteAddress().port();
            } else {
                this.peer = "unknown";
            }
        }

        @Override
        public void sendMessage(String message) {
            if (log.isTraceEnabled()) {
                log.trace(">>> " + message);
            }
            webSocket.writeTextMessage(message);
        }

        @Override
        public void close(short statusCode, String reason) {
            if (log.isDebugEnabled()) {
                log.debug("Closing graphql websocket connection with code=" + statusCode + " and reason " + reason);
            }
            webSocket.close(statusCode, reason);
        }

        @Override
        public boolean isClosed() {
            return webSocket.isClosed();
        }

        @Override
        public String toString() {
            return "{peer=" + peer + "}";
        }
    }

}
