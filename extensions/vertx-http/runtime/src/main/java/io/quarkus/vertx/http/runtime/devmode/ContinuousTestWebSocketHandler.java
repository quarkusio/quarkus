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
                Json.JsonObjectBuilder response = Json.object();
                response.put("running", state.running);
                response.put("inProgress", state.inProgress);
                response.put("run", state.run);
                response.put("passed", state.passed);
                response.put("failed", state.failed);
                response.put("skipped", state.skipped);
                response.put("isBrokenOnly", state.isBrokenOnly);
                response.put("isTestOutput", state.isTestOutput);
                response.put("isInstrumentationBasedReload", state.isInstrumentationBasedReload);

                lastMessage = response.build();
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
