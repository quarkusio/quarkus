package io.quarkus.websockets.next.runtime;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.WebSocketsServerRuntimeConfig;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Route;
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

    public Consumer<Route> initializeSecurityHandler() {
        return new Consumer<Route>() {

            @Override
            public void accept(Route route) {
                // Force authentication so that it's possible to capture the SecurityIdentity before the HTTP upgrade
                route.handler(new Handler<RoutingContext>() {

                    @Override
                    public void handle(RoutingContext ctx) {
                        if (ctx.user() == null) {
                            Uni<SecurityIdentity> deferredIdentity = ctx
                                    .<Uni<SecurityIdentity>> get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY);
                            deferredIdentity.subscribe().with(i -> {
                                if (ctx.response().ended()) {
                                    return;
                                }
                                ctx.next();
                            }, ctx::fail);
                        } else {
                            ctx.next();
                        }
                    }
                });
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

                    SecuritySupport securitySupport = initializeSecuritySupport(container, ctx, vertx, connection);

                    Endpoints.initialize(vertx, container, codecs, connection, ws, generatedEndpointClass,
                            config.autoPingInterval(), securitySupport, config.unhandledFailureStrategy(),
                            () -> connectionManager.remove(generatedEndpointClass, connection));
                });
            }
        };
    }

    SecuritySupport initializeSecuritySupport(ArcContainer container, RoutingContext ctx, Vertx vertx,
            WebSocketConnectionImpl connection) {
        Instance<CurrentIdentityAssociation> currentIdentityAssociation = container.select(CurrentIdentityAssociation.class);
        if (currentIdentityAssociation.isResolvable()) {
            // Security extension is present
            // Obtain the current security identity from the handshake request
            QuarkusHttpUser user = (QuarkusHttpUser) ctx.user();
            if (user != null) {
                return new SecuritySupport(currentIdentityAssociation, user.getSecurityIdentity(), vertx, connection);
            }
        }
        return SecuritySupport.NOOP;
    }

}
