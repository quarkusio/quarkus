package io.quarkus.websockets.next.runtime;

import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.WebSocketsServerRuntimeConfig;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class WebSocketServerRecorder {

    private static final Logger LOG = Logger.getLogger(WebSocketServerRecorder.class);

    private final WebSocketsServerRuntimeConfig config;

    public WebSocketServerRecorder(WebSocketsServerRuntimeConfig config) {
        this.config = config;
    }

    public Supplier<Object> connectionSupplier() {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                Context context = Vertx.currentContext();
                if (context != null && VertxContext.isDuplicatedContext(context)) {
                    Object connection = context.getLocal(ContextSupport.WEB_SOCKET_CONN_KEY);
                    if (connection != null) {
                        return connection;
                    }
                }
                throw new WebSocketServerException("Unable to obtain the connection from the Vert.x duplicated context");
            }
        };
    }

    public Handler<RoutingContext> createEndpointHandler(String generatedEndpointClass, String endpointId) {
        ArcContainer container = Arc.container();
        ConnectionManager connectionManager = container.instance(ConnectionManager.class).get();
        Codecs codecs = container.instance(Codecs.class).get();
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                Future<ServerWebSocket> future = ctx.request().toWebSocket();
                future.onSuccess(ws -> {
                    Vertx vertx = VertxCoreRecorder.getVertx().get();

                    WebSocketConnectionImpl connection = new WebSocketConnectionImpl(generatedEndpointClass, endpointId, ws,
                            connectionManager, codecs, ctx);
                    connectionManager.add(generatedEndpointClass, connection);
                    LOG.debugf("Connection created: %s", connection);

                    Endpoints.initialize(vertx, container, codecs, connection, ws, generatedEndpointClass,
                            config.autoPingInterval(), () -> connectionManager.remove(generatedEndpointClass, connection));
                });
            }
        };
    }

}
