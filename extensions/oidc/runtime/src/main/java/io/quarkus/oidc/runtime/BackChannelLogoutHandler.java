package io.quarkus.oidc.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.SecurityEvent.Type;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Singleton
public final class BackChannelLogoutHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(BackChannelLogoutHandler.class);
    private final DefaultTenantConfigResolver resolver;
    private volatile ImmutablePathMatcher<Handler<RoutingContext>> pathMatcher;

    record NewBackChannelLogoutPath() {
    }

    BackChannelLogoutHandler(DefaultTenantConfigResolver resolver) {
        this.resolver = resolver;
        this.pathMatcher = null;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        var matcher = pathMatcher;
        if (matcher != null) {
            Handler<RoutingContext> routeHandler = matcher.match(routingContext.normalizedPath()).getValue();
            if (routeHandler != null) {
                routeHandler.handle(routingContext);
                return;
            }
        }

        routingContext.next();
    }

    // hook up to the router because then the tenant config bean is surely ready
    void createPathMatcher(@Observes Router ignored) {
        createOrUpdatePathMatcher();
    }

    synchronized void updatePathMatcher(@Observes NewBackChannelLogoutPath ignored, Vertx vertx) {
        Set<String> currentTenantIds = createOrUpdatePathMatcher();
        clearCache(vertx, currentTenantIds);
    }

    void clearCacheOnShutdown(@Observes ShutdownEvent event, Vertx vertx) {
        clearCache(vertx, null);
    }

    private void clearCache(Vertx vertx, Set<String> currentTenantIds) {
        if (currentTenantIds == null) {
            // clear all as currently we have no tenants with a back-channel logout path
            for (BackChannelLogoutTokenCache cache : resolver.getBackChannelLogoutTokens().values()) {
                cache.shutdown(vertx);
            }
            resolver.getBackChannelLogoutTokens().clear();
        } else {
            // clear only the ones that currently don't have a logout back-channel
            Set<String> cachedTenantIds = new HashSet<>(resolver.getBackChannelLogoutTokens().keySet());
            for (String cachedTenantId : cachedTenantIds) {
                if (!currentTenantIds.contains(cachedTenantId)) {
                    var cache = resolver.getBackChannelLogoutTokens().remove(cachedTenantId);
                    if (cache != null) {
                        cache.shutdown(vertx);
                    }
                }
            }
        }
    }

    private Set<String> createOrUpdatePathMatcher() {
        ImmutablePathMatcher.ImmutablePathMatcherBuilder<Handler<RoutingContext>> builder = null;
        Map<String, OidcTenantConfig> pathCache = null;
        Set<String> tenantIdCache = null;
        for (TenantConfigContext configContext : resolver.getTenantConfigBean().getAllTenantConfigs()) {
            if (configContext.ready() && configContext.oidcConfig().tenantEnabled()
                    && configContext.oidcConfig().logout().backchannel().path().isPresent()) {
                if (builder == null) {
                    builder = ImmutablePathMatcher.builder();
                    pathCache = new HashMap<>();
                    tenantIdCache = new HashSet<>();
                }
                String routePath = getTenantLogoutPath(configContext);
                if (routePath.contains("*")) {
                    throw new IllegalStateException("Back-channel logout path cannot contain a wildcard '*' character");
                }
                OidcTenantConfig previousConfig = pathCache.put(routePath, configContext.oidcConfig());
                tenantIdCache.add(configContext.oidcConfig().tenantId().get());
                if (previousConfig == null) {
                    Handler<RoutingContext> routeHandler = new RouteHandler(configContext, resolver);
                    builder.addPath(routePath, routeHandler);
                } else {
                    String previousTenantId = previousConfig.tenantId().get();
                    String currentTenantId = configContext.oidcConfig().tenantId().get();
                    // maybe invalid state, but technically it could happen that some produces a static tenant with
                    // a same id as a dynamic tenant
                    if (!previousTenantId.equals(currentTenantId)) {
                        String errorMessage = "OIDC tenants '%s' and '%s' share the same back-channel logout path '%s', which is not supported"
                                .formatted(previousTenantId, currentTenantId, routePath);
                        LOG.error(errorMessage);
                        throw new OIDCException(errorMessage);
                    }
                }
            }
        }
        if (builder != null) {
            pathMatcher = builder.build();
        } else {
            pathMatcher = null;
        }
        return tenantIdCache;
    }

    private String getTenantLogoutPath(TenantConfigContext tenant) {
        return OidcUtils.getRootPath(resolver.getRootPath()) + tenant.oidcConfig().logout().backchannel().path().orElse(null);
    }

    private static final class RouteHandler implements Handler<RoutingContext> {
        private final TenantConfigContext tenantContext;
        private final DefaultTenantConfigResolver resolver;
        private final String tenantId;

        private RouteHandler(TenantConfigContext tenantContext, DefaultTenantConfigResolver resolver) {
            this.tenantContext = tenantContext;
            this.resolver = resolver;
            this.tenantId = tenantContext.oidcConfig().tenantId().get();
        }

        @Override
        public void handle(RoutingContext context) {
            LOG.debugf("Back channel logout request for the tenant %s received", tenantId);
            if (OidcUtils.isFormUrlEncodedRequest(context)) {
                OidcUtils.getFormUrlEncodedData(context)
                        .subscribe().with(new Consumer<MultiMap>() {
                            @Override
                            public void accept(MultiMap form) {

                                String encodedLogoutToken = form.get(OidcConstants.BACK_CHANNEL_LOGOUT_TOKEN);
                                if (encodedLogoutToken == null) {
                                    LOG.debug("Back channel logout token is missing");
                                    context.response().setStatusCode(400);
                                } else {
                                    try {
                                        // Do the general validation of the logout token now, compare with the IDToken later
                                        // Check the signature, as well the issuer and audience if it is configured
                                        TokenVerificationResult result = tenantContext.provider()
                                                .verifyLogoutJwtToken(encodedLogoutToken);

                                        if (verifyLogoutTokenClaims(result)) {
                                            String key = result.localVerificationResult
                                                    .getString(
                                                            tenantContext.oidcConfig().logout().backchannel().logoutTokenKey());
                                            BackChannelLogoutTokenCache tokens = resolver.getBackChannelLogoutTokens()
                                                    .get(tenantId);
                                            if (tokens == null) {
                                                tokens = new BackChannelLogoutTokenCache(tenantContext.oidcConfig(),
                                                        context.vertx());
                                                resolver.getBackChannelLogoutTokens().put(tenantId, tokens);
                                            }
                                            tokens.addTokenVerification(key, result);

                                            if (resolver.isSecurityEventObserved()) {
                                                SecurityEventHelper.fire(resolver.getSecurityEvent(),
                                                        new SecurityEvent(Type.OIDC_BACKCHANNEL_LOGOUT_INITIATED,
                                                                Map.of(OidcConstants.BACK_CHANNEL_LOGOUT_TOKEN, result)));
                                            }
                                            context.response().setStatusCode(200);
                                        } else {
                                            context.response().setStatusCode(400);
                                        }
                                    } catch (InvalidJwtException e) {
                                        LOG.debug("Back channel logout token is invalid");
                                        context.response().setStatusCode(400);

                                    }
                                }
                                context.response().end();
                            }

                        });

            } else {
                LOG.debug("HTTP POST and " + HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString()
                        + " content type must be used with the Back channel logout request");
                context.response().setStatusCode(400);
                context.response().end();
            }
        }

        private boolean verifyLogoutTokenClaims(TokenVerificationResult result) {
            // events
            JsonObject events = result.localVerificationResult.getJsonObject(OidcConstants.BACK_CHANNEL_EVENTS_CLAIM);
            if (events == null || events.getJsonObject(OidcConstants.BACK_CHANNEL_EVENT_NAME) == null) {
                LOG.debug("Back channel logout token does not have a valid 'events' claim");
                return false;
            }
            if (!result.localVerificationResult
                    .containsKey(tenantContext.oidcConfig().logout().backchannel().logoutTokenKey())) {
                LOG.debugf("Back channel logout token does not have %s",
                        tenantContext.oidcConfig().logout().backchannel().logoutTokenKey());
                return false;
            }
            if (result.localVerificationResult.containsKey(Claims.nonce.name())) {
                LOG.debug("Back channel logout token must not contain 'nonce' claim");
                return false;
            }

            return true;
        }
    }

    public static void fireBackChannelLogoutChangedEvent(OidcTenantConfig oidcConfig, TenantConfigContext tenant) {
        if (oidcConfig.logout().backchannel().path().isPresent()) {
            boolean pathChanged = tenant.oidcConfig() == null || !oidcConfig.logout().backchannel().path().get()
                    .equals(tenant.oidcConfig().logout().backchannel().path().orElse(null));
            if (pathChanged) {
                fireBackChannelLogoutEvent();
            }
        }

    }

    public static void fireBackChannelLogoutReadyEvent(OidcTenantConfig oidcConfig) {
        if (oidcConfig.logout().backchannel().path().isPresent()) {
            fireBackChannelLogoutEvent();
        }

    }

    private static void fireBackChannelLogoutEvent() {
        Event<NewBackChannelLogoutPath> event = Arc.container().beanManager().getEvent()
                .select(NewBackChannelLogoutPath.class);
        event.fire(new NewBackChannelLogoutPath());
    }

}
