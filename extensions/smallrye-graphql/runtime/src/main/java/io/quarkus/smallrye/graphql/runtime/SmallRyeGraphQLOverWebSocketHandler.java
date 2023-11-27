package io.quarkus.smallrye.graphql.runtime;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.graphql.websocket.GraphQLWebSocketSession;
import io.smallrye.graphql.websocket.GraphQLWebsocketHandler;
import io.smallrye.graphql.websocket.graphqltransportws.GraphQLTransportWSSubprotocolHandler;
import io.smallrye.graphql.websocket.graphqlws.GraphQLWSSubprotocolHandler;
import io.vertx.core.Handler;
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
            Map<String, Object> metaData = getMetaData(ctx);
            ctx.request().toWebSocket(event -> {
                if (event.succeeded()) {
                    ServerWebSocket serverWebSocket = event.result();
                    String subprotocol = serverWebSocket.subProtocol();
                    if (subprotocol == null) {
                        log.warn("Websocket subprotocol is null");
                        serverWebSocket.close();
                        return;
                    }
                    GraphQLWebsocketHandler handler;
                    switch (subprotocol) {
                        case "graphql-transport-ws":
                            handler = new GraphQLTransportWSSubprotocolHandler(
                                    new QuarkusVertxWebSocketSession(serverWebSocket), metaData);
                            break;
                        case "graphql-ws":
                            handler = new GraphQLWSSubprotocolHandler(
                                    new QuarkusVertxWebSocketSession(serverWebSocket), metaData);
                            break;
                        default:
                            log.warn("Unknown graphql-over-websocket protocol: " + subprotocol);
                            serverWebSocket.close();
                            return;
                    }

                    QuarkusHttpUser user = (QuarkusHttpUser) ctx.user();
                    long cancellation = -1L; // Do not use 0, as you won't be able to distinguish between not set, and the first task Id
                    if (user != null) {
                        //close the connection when the identity expires
                        Long expire = user.getSecurityIdentity().getAttribute("quarkus.identity.expire-time");
                        if (expire != null) {
                            cancellation = ctx.vertx().setTimer((expire * 1000) - System.currentTimeMillis(),
                                    new Handler<Long>() {
                                        @Override
                                        public void handle(Long event) {
                                            if (!serverWebSocket.isClosed()) {
                                                serverWebSocket.close((short) 1008, "Authentication expired");
                                            }
                                        }
                                    });
                        }
                    }

                    log.debugf("Starting websocket with subprotocol = %s", subprotocol);
                    GraphQLWebsocketHandler finalHandler = handler;
                    long finalCancellation = cancellation;
                    serverWebSocket.closeHandler(v -> {
                        finalHandler.onClose();
                        if (finalCancellation != -1) {
                            ctx.vertx().cancelTimer(finalCancellation);
                        }
                    });
                    serverWebSocket.endHandler(v -> finalHandler.onEnd());
                    serverWebSocket.exceptionHandler(finalHandler::onThrowable);
                    serverWebSocket.textMessageHandler(finalHandler::onMessage);
                } else {
                    log.warn("Websocket failed", event.cause());
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
