package io.quarkus.oidc.runtime;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.SecurityEvent.Type;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class BackChannelLogoutHandler {
    private static final Logger LOG = Logger.getLogger(BackChannelLogoutHandler.class);
    private static final String SLASH = "/";

    @Inject
    DefaultTenantConfigResolver resolver;

    private final OidcConfig oidcConfig;

    public BackChannelLogoutHandler(OidcConfig oidcConfig) {
        this.oidcConfig = oidcConfig;
    }

    public void setup(@Observes Router router) {
        addRoute(router, oidcConfig.defaultTenant);

        for (OidcTenantConfig oidcTenantConfig : oidcConfig.namedTenants.values()) {
            addRoute(router, oidcTenantConfig);
        }
    }

    private void addRoute(Router router, OidcTenantConfig oidcTenantConfig) {
        if (oidcTenantConfig.isTenantEnabled() && oidcTenantConfig.logout.backchannel.path.isPresent()) {
            router.route(oidcTenantConfig.logout.backchannel.path.get())
                    .handler(new RouteHandler(oidcTenantConfig));
        }
    }

    class RouteHandler implements Handler<RoutingContext> {
        private final OidcTenantConfig oidcTenantConfig;

        RouteHandler(OidcTenantConfig oidcTenantConfig) {
            this.oidcTenantConfig = oidcTenantConfig;
        }

        @Override
        public void handle(RoutingContext context) {
            LOG.debugf("Back channel logout request for the tenant %s received", oidcTenantConfig.getTenantId().get());
            final String requestPath = context.request().path();
            final TenantConfigContext tenantContext = getTenantConfigContext(requestPath);
            if (tenantContext == null) {
                LOG.errorf(
                        "Tenant configuration for the tenant %s is not available "
                                + "or does not match the backchannel logout path %s",
                        oidcTenantConfig.getTenantId().get(), requestPath);
                context.response().setStatusCode(400);
                context.response().end();
                return;
            }

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
                                                    .getString(oidcTenantConfig.logout.backchannel.logoutTokenKey);
                                            BackChannelLogoutTokenCache tokens = resolver
                                                    .getBackChannelLogoutTokens().get(oidcTenantConfig.tenantId.get());
                                            if (tokens == null) {
                                                tokens = new BackChannelLogoutTokenCache(oidcTenantConfig, context.vertx());
                                                resolver.getBackChannelLogoutTokens().put(oidcTenantConfig.tenantId.get(),
                                                        tokens);
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
            if (!result.localVerificationResult.containsKey(oidcTenantConfig.logout.backchannel.logoutTokenKey)) {
                LOG.debugf("Back channel logout token does not have %s", oidcTenantConfig.logout.backchannel.logoutTokenKey);
                return false;
            }
            if (result.localVerificationResult.containsKey(Claims.nonce.name())) {
                LOG.debug("Back channel logout token must not contain 'nonce' claim");
                return false;
            }

            return true;
        }

        private TenantConfigContext getTenantConfigContext(final String requestPath) {
            if (isMatchingTenant(requestPath, resolver.getTenantConfigBean().getDefaultTenant())) {
                return resolver.getTenantConfigBean().getDefaultTenant();
            }
            for (TenantConfigContext tenant : resolver.getTenantConfigBean().getStaticTenantsConfig().values()) {
                if (isMatchingTenant(requestPath, tenant)) {
                    return tenant;
                }
            }
            return null;
        }

        private boolean isMatchingTenant(String requestPath, TenantConfigContext tenant) {
            return tenant.oidcConfig().isTenantEnabled()
                    && tenant.oidcConfig().getTenantId().get().equals(oidcTenantConfig.getTenantId().get())
                    && requestPath.equals(getRootPath() + tenant.oidcConfig().logout.backchannel.path.orElse(null));
        }
    }

    private String getRootPath() {
        // Prepend '/' if it is not present
        String rootPath = OidcCommonUtils.prependSlash(resolver.getRootPath());
        // Strip trailing '/' if the length is > 1
        if (rootPath.length() > 1 && rootPath.endsWith("/")) {
            rootPath = rootPath.substring(rootPath.length() - 1);
        }
        // if it is only '/' then return an empty value
        return SLASH.equals(rootPath) ? "" : rootPath;
    }
}
