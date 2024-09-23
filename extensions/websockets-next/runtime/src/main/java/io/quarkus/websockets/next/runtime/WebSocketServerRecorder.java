package io.quarkus.websockets.next.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.HttpUpgradeCheck.CheckResult;
import io.quarkus.websockets.next.HttpUpgradeCheck.HttpUpgradeContext;
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
        HttpUpgradeCheck[] httpUpgradeChecks = getHttpUpgradeChecks(endpointId, container);
        TrafficLogger trafficLogger = TrafficLogger.forServer(config);
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                if (!ctx.request().headers().contains(HandshakeRequest.SEC_WEBSOCKET_KEY)) {
                    LOG.debugf("Non-websocket client request ignored:\n%s", ctx.request().headers());
                    ctx.next();
                }
                if (httpUpgradeChecks != null) {
                    checkHttpUpgrade(ctx, endpointId).subscribe().with(result -> {
                        if (!result.getResponseHeaders().isEmpty()) {
                            result.getResponseHeaders().forEach((k, v) -> ctx.response().putHeader(k, v));
                        }

                        if (result.isUpgradePermitted()) {
                            httpUpgrade(ctx);
                        } else {
                            ctx.response().setStatusCode(result.getHttpResponseCode()).end();
                        }
                    }, ctx::fail);
                } else {
                    httpUpgrade(ctx);
                }
            }

            private void httpUpgrade(RoutingContext ctx) {
                Future<ServerWebSocket> future = ctx.request().toWebSocket();
                future.onSuccess(ws -> {
                    Vertx vertx = VertxCoreRecorder.getVertx().get();

                    WebSocketConnectionImpl connection = new WebSocketConnectionImpl(generatedEndpointClass, endpointId, ws,
                            connectionManager, codecs, ctx, trafficLogger);
                    connectionManager.add(generatedEndpointClass, connection);
                    if (trafficLogger != null) {
                        trafficLogger.connectionOpened(connection);
                    }

                    SecuritySupport securitySupport = initializeSecuritySupport(container, ctx, vertx, connection);

                    Endpoints.initialize(vertx, container, codecs, connection, ws, generatedEndpointClass,
                            config.autoPingInterval(), securitySupport, config.unhandledFailureStrategy(), trafficLogger,
                            () -> connectionManager.remove(generatedEndpointClass, connection));
                });
            }

            private Uni<CheckResult> checkHttpUpgrade(RoutingContext ctx, String endpointId) {
                SecurityIdentity identity = ctx.user() instanceof QuarkusHttpUser user ? user.getSecurityIdentity() : null;
                return checkHttpUpgrade(new HttpUpgradeContext(ctx.request(), identity, endpointId), httpUpgradeChecks, 0);
            }

            private static Uni<CheckResult> checkHttpUpgrade(HttpUpgradeContext ctx,
                    HttpUpgradeCheck[] checks, int idx) {
                return checks[idx].perform(ctx).flatMap(res -> {
                    if (res == null) {
                        return Uni.createFrom().failure(new IllegalStateException(
                                "The '%s' returned null CheckResult, please make sure non-null value is returned"
                                        .formatted(checks[idx])));
                    }
                    if (idx < checks.length - 1 && res.isUpgradePermitted()) {
                        return checkHttpUpgrade(ctx, checks, idx + 1)
                                .map(n -> n.withHeaders(res.getResponseHeaders()));
                    }
                    return Uni.createFrom().item(res);
                });
            }
        };
    }

    private static HttpUpgradeCheck[] getHttpUpgradeChecks(String endpointId, ArcContainer container) {
        List<HttpUpgradeCheck> httpUpgradeChecks = null;
        for (var check : container.select(HttpUpgradeCheck.class)) {
            if (!check.appliesTo(endpointId)) {
                continue;
            }
            if (httpUpgradeChecks == null) {
                httpUpgradeChecks = new ArrayList<>();
            }
            httpUpgradeChecks.add(check);
        }
        return httpUpgradeChecks == null ? null : httpUpgradeChecks.toArray(new HttpUpgradeCheck[0]);
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

    public Supplier<HttpUpgradeCheck> createSecurityHttpUpgradeCheck(Map<String, SecurityCheck> endpointToCheck) {
        return new Supplier<HttpUpgradeCheck>() {
            @Override
            public HttpUpgradeCheck get() {
                return new SecurityHttpUpgradeCheck(config.security().authFailureRedirectUrl().orElse(null), endpointToCheck);
            }
        };
    }
}
