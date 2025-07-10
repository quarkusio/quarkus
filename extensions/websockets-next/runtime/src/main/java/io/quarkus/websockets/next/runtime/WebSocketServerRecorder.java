package io.quarkus.websockets.next.runtime;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorStorage;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.HttpUpgradeCheck.CheckResult;
import io.quarkus.websockets.next.HttpUpgradeCheck.HttpUpgradeContext;
import io.quarkus.websockets.next.WebSocketSecurity;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.runtime.config.WebSocketsServerRuntimeConfig;
import io.quarkus.websockets.next.runtime.spi.security.WebSocketIdentityUpdateRequest;
import io.quarkus.websockets.next.runtime.telemetry.SendingInterceptor;
import io.quarkus.websockets.next.runtime.telemetry.WebSocketTelemetryProvider;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class WebSocketServerRecorder {
    private static final Logger LOG = Logger.getLogger(WebSocketServerRecorder.class);

    private final RuntimeValue<WebSocketsServerRuntimeConfig> runtimeConfig;

    public WebSocketServerRecorder(final RuntimeValue<WebSocketsServerRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
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

    public Handler<RoutingContext> createEndpointHandler(String generatedEndpointClass, String endpointId,
            boolean activateRequestContext, boolean activateSessionContext, String endpointPath) {
        ArcContainer container = Arc.container();
        ConnectionManager connectionManager = container.instance(ConnectionManager.class).get();
        Codecs codecs = container.instance(Codecs.class).get();
        HttpUpgradeCheck[] httpUpgradeChecks = getHttpUpgradeChecks(endpointId, container);
        TrafficLogger trafficLogger = TrafficLogger.forServer(runtimeConfig.getValue());
        WebSocketTelemetryProvider telemetryProvider = container.instance(WebSocketTelemetryProvider.class).orElse(null);
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                if (ctx.request().headers().contains(HandshakeRequest.SEC_WEBSOCKET_KEY)) {
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
                } else {
                    LOG.debugf("Non-websocket client request ignored:\n%s", ctx.request().headers());
                    ctx.next();
                }
            }

            private void httpUpgrade(RoutingContext ctx) {
                var telemetrySupport = telemetryProvider == null ? null
                        : telemetryProvider.createServerTelemetrySupport(endpointPath);
                final Future<ServerWebSocket> future;
                if (telemetrySupport != null && telemetrySupport.interceptConnection()) {
                    telemetrySupport.connectionOpened();
                    future = ctx.request().toWebSocket().onFailure(new Handler<Throwable>() {
                        @Override
                        public void handle(Throwable throwable) {
                            telemetrySupport.connectionOpeningFailed(throwable);
                        }
                    });
                } else {
                    future = ctx.request().toWebSocket();
                }

                future.onSuccess(ws -> {
                    Vertx vertx = VertxCoreRecorder.getVertx().get();

                    SendingInterceptor sendingInterceptor = telemetrySupport == null ? null
                            : telemetrySupport.getSendingInterceptor();
                    WebSocketConnectionImpl connection = new WebSocketConnectionImpl(generatedEndpointClass, endpointId, ws,
                            connectionManager, codecs, ctx, trafficLogger, sendingInterceptor,
                            getSecuritySupportCreator(container, ctx));
                    connectionManager.add(generatedEndpointClass, connection);
                    if (trafficLogger != null) {
                        trafficLogger.connectionOpened(connection);
                    }

                    Endpoints.initialize(vertx, container, codecs, connection, ws, generatedEndpointClass,
                            runtimeConfig.getValue().autoPingInterval(), connection.securitySupport(),
                            runtimeConfig.getValue().unhandledFailureStrategy(), trafficLogger,
                            () -> connectionManager.remove(generatedEndpointClass, connection), activateRequestContext,
                            activateSessionContext, telemetrySupport);
                });
            }

            private Uni<CheckResult> checkHttpUpgrade(RoutingContext ctx, String endpointId) {
                QuarkusHttpUser user = (QuarkusHttpUser) ctx.user();
                Uni<SecurityIdentity> identity;
                if (user == null) {
                    identity = ctx.<Uni<SecurityIdentity>> get(QuarkusHttpUser.DEFERRED_IDENTITY_KEY);
                } else {
                    identity = Uni.createFrom().item(user.getSecurityIdentity());
                }
                return checkHttpUpgrade(new HttpUpgradeContextImpl(ctx, identity, endpointId), httpUpgradeChecks, 0);
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

    private static Function<WebSocketConnectionImpl, SecuritySupport> getSecuritySupportCreator(ArcContainer container,
            RoutingContext ctx) {
        Instance<CurrentIdentityAssociation> currentIdentityAssociation = container.select(CurrentIdentityAssociation.class);
        if (currentIdentityAssociation.isResolvable()) {
            // Security extension is present
            // Obtain the current security identity from the handshake request
            if (ctx.user() instanceof QuarkusHttpUser user) {
                return connection -> new SecuritySupport(user.getSecurityIdentity(), connection, ctx);
            }
        }
        return ignored -> SecuritySupport.NOOP;
    }

    public Function<SyntheticCreationalContext<SecurityHttpUpgradeCheck>, SecurityHttpUpgradeCheck> createSecurityHttpUpgradeCheck(
            Map<String, SecurityCheck> endpointToCheck) {
        return new Function<SyntheticCreationalContext<SecurityHttpUpgradeCheck>, SecurityHttpUpgradeCheck>() {
            @Override
            public SecurityHttpUpgradeCheck apply(SyntheticCreationalContext<SecurityHttpUpgradeCheck> ctx) {
                boolean securityEventsEnabled = ConfigProvider.getConfig().getValue("quarkus.security.events.enabled",
                        Boolean.class);
                var securityEventHelper = new SecurityEventHelper<>(ctx.getInjectedReference(new TypeLiteral<>() {
                }), ctx.getInjectedReference(new TypeLiteral<>() {
                }), AUTHORIZATION_SUCCESS,
                        AUTHORIZATION_FAILURE, ctx.getInjectedReference(BeanManager.class), securityEventsEnabled);
                WebSocketsServerRuntimeConfig config = ctx.getInjectedReference(WebSocketsServerRuntimeConfig.class);
                return new SecurityHttpUpgradeCheck(config.security().authFailureRedirectUrl().orElse(null), endpointToCheck,
                        securityEventHelper);
            }
        };
    }

    public Function<SyntheticCreationalContext<HttpUpgradeSecurityInterceptor>, HttpUpgradeSecurityInterceptor> createHttpUpgradeSecurityInterceptor(
            Map<String, String> classNameToEndpointId) {
        return new Function<SyntheticCreationalContext<HttpUpgradeSecurityInterceptor>, HttpUpgradeSecurityInterceptor>() {
            @Override
            public HttpUpgradeSecurityInterceptor apply(SyntheticCreationalContext<HttpUpgradeSecurityInterceptor> ctx) {
                EagerSecurityInterceptorStorage storage = ctx.getInjectedReference(EagerSecurityInterceptorStorage.class);
                Map<String, Consumer<RoutingContext>> endpointIdToInterceptor = new HashMap<>();
                classNameToEndpointId.forEach((className, endpointId) -> {
                    Consumer<RoutingContext> interceptor = Objects.requireNonNull(storage.getClassInterceptor(className));
                    endpointIdToInterceptor.put(endpointId, interceptor);
                });
                return new HttpUpgradeSecurityInterceptor(endpointIdToInterceptor);
            }
        };
    }

    public Function<SyntheticCreationalContext<WebSocketSecurity>, WebSocketSecurity> createWebSocketSecurity() {
        final Supplier<Object> connectionSupplier = connectionSupplier();
        return new Function<SyntheticCreationalContext<WebSocketSecurity>, WebSocketSecurity>() {
            @Override
            public WebSocketSecurity apply(SyntheticCreationalContext<WebSocketSecurity> ctx) {
                Instance<IdentityProvider<?>> identityProviders = ctx.getInjectedReference(new TypeLiteral<>() {
                });
                boolean updateNotSupported = true;
                for (IdentityProvider<?> identityProvider : identityProviders) {
                    if (identityProvider.getRequestType() == WebSocketIdentityUpdateRequest.class) {
                        updateNotSupported = false;
                        break;
                    }
                }
                if (updateNotSupported) {
                    throw new WebSocketServerException("""
                            The '%s' CDI bean injection point was detected, but there is no '%s' that supports '%s'.
                            Either add Quarkus extension that supports SecurityIdentity update like Quarkus OIDC, or
                            implement the provider yourself.
                            """.formatted(WebSocketSecurity.class.getName(), IdentityProvider.class.getName(),
                            WebSocketIdentityUpdateRequest.class.getName()));
                }
                final IdentityProviderManager identityProviderManager = ctx.getInjectedReference(IdentityProviderManager.class);
                return new WebSocketSecurity() {
                    @Override
                    public CompletionStage<SecurityIdentity> updateSecurityIdentity(String accessToken) {
                        if (connectionSupplier.get() instanceof WebSocketConnectionImpl connection) {
                            SecuritySupport securitySupport = connection.securitySupport();
                            return securitySupport.updateSecurityIdentity(accessToken, connection, identityProviderManager);
                        }
                        throw new WebSocketServerException(
                                "Only SecurityIdentity attached to a WebSocket server connection can be updated");
                    }
                };
            }
        };
    }
}
