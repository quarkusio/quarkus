package io.quarkus.grpc.runtime.devmode;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.dev.devui.DevConsoleManager;
import io.quarkus.dev.testing.GrpcWebSocketProxy;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class GrpcDevConsoleRecorder {
    private static final Logger log = Logger.getLogger(GrpcDevConsoleRecorder.class);

    public void setServerConfiguration() {
        try (InstanceHandle<GrpcConfiguration> config = Arc.container().instance(GrpcConfiguration.class)) {
            GrpcServerConfiguration serverConfig = config.get().server;
            Map<String, Object> map = new HashMap<>();
            map.put("host", serverConfig.host);
            map.put("port", serverConfig.port);
            map.put("ssl", serverConfig.ssl.certificate.isPresent() || serverConfig.ssl.keyStore.isPresent());
            DevConsoleManager.setGlobal("io.quarkus.grpc.serverConfig", map);
        }
    }

    public Handler<RoutingContext> handler() {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext context) {
                context.request().toWebSocket(webSocket -> {
                    if (webSocket.failed()) {
                        log.error("failed to connect web socket", webSocket.cause());
                    } else {
                        ServerWebSocket serverWebSocket = webSocket.result();
                        Integer socketId = GrpcWebSocketProxy.addWebSocket(
                                message -> serverWebSocket.writeTextMessage(message)
                                        .onFailure(e -> log
                                                .info("failed to send back message to the gRPC Dev Console WebSocket", e)),
                                runnable -> {
                                    if (!serverWebSocket.isClosed()) {
                                        serverWebSocket.close(new Handler<AsyncResult<Void>>() {
                                            @Override
                                            public void handle(AsyncResult<Void> event) {
                                                runnable.run();
                                            }
                                        });
                                    } else {
                                        runnable.run();
                                    }
                                });

                        if (socketId == null) {
                            log.error("No gRPC dev console WebSocketListener");
                            serverWebSocket.close();
                            return;
                        }
                        serverWebSocket.closeHandler(ignored -> GrpcWebSocketProxy.closeWebSocket(socketId));
                        serverWebSocket.handler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer event) {
                                GrpcWebSocketProxy.addMessage(socketId, event.toString());
                            }
                        });
                    }
                });
            }
        };
    }
}
