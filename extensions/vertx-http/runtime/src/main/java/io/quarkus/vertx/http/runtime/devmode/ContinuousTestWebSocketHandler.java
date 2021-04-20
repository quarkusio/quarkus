package io.quarkus.vertx.http.runtime.devmode;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.dev.testing.ContinuousTestingWebsocketListener;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

public class ContinuousTestWebSocketHandler implements Handler<RoutingContext> {

    private static final Logger log = Logger.getLogger(ContinuousTestingWebsocketListener.class);
    private static final Set<ServerWebSocket> sockets = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static volatile String lastMessage;

    static {
        ContinuousTestingWebsocketListener.setStateListener(new Consumer<ContinuousTestingWebsocketListener.State>() {
            @Override
            public void accept(ContinuousTestingWebsocketListener.State state) {
                StringBuilder sb = new StringBuilder();
                //we don't have JSON support so we manually build it
                //pretty yuck
                //maybe we should try and move this to the deployment side somehow
                sb.append("{\"running\":");
                sb.append(state.running);
                sb.append(", \"inProgress\":");
                sb.append(state.inProgress);
                sb.append(", \"run\":");
                sb.append(state.run);
                sb.append(", \"passed\":");
                sb.append(state.passed);
                sb.append(", \"failed\":");
                sb.append(state.failed);
                sb.append(", \"skipped\":");
                sb.append(state.skipped);
                sb.append("}");
                lastMessage = sb.toString();
                for (ServerWebSocket i : sockets) {
                    i.writeTextMessage(lastMessage);
                }
            }
        });
    }

    @Override
    public void handle(RoutingContext event) {

        if ("websocket".equalsIgnoreCase(event.request().getHeader(HttpHeaderNames.UPGRADE))) {
            event.request().toWebSocket(new Handler<AsyncResult<ServerWebSocket>>() {
                @Override
                public void handle(AsyncResult<ServerWebSocket> event) {
                    if (event.succeeded()) {
                        ServerWebSocket socket = event.result();
                        if (lastMessage != null) {
                            socket.writeTextMessage(lastMessage);
                        }
                        sockets.add(socket);
                        socket.closeHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                sockets.remove(socket);
                            }
                        });
                    } else {
                        log.error("Failed to connect to test server", event.cause());
                    }
                }
            });
        } else {
            event.next();
        }
    }
}
