package io.quarkus.oidc.runtime;

import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class BackChannelLogoutHandler {
    private static final Logger LOG = Logger.getLogger(BackChannelLogoutHandler.class);

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
            router.route(oidcTenantConfig.logout.backchannel.path.get()).handler(new RouteHandler(oidcTenantConfig));
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
            final TenantConfigContext tenantContext = getTenantConfigContext(context);
            if (tenantContext == null) {
                LOG.debugf(
                        "Tenant configuration for the tenant %s is not available or does not match the backchannel logout path",
                        oidcTenantConfig.getTenantId().get());
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
                                        TokenVerificationResult result = tenantContext.provider
                                                .verifyLogoutJwtToken(encodedLogoutToken);

                                        if (verifyLogoutTokenClaims(result)) {
                                            resolver.getBackChannelLogoutTokens().put(oidcTenantConfig.tenantId.get(),
                                                    result);
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
            if (!result.localVerificationResult.containsKey(Claims.sub.name())
                    && !result.localVerificationResult.containsKey(OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM)) {
                LOG.debug("Back channel logout token does not have 'sub' or 'sid' claim");
                return false;
            }
            if (result.localVerificationResult.containsKey(Claims.nonce.name())) {
                LOG.debug("Back channel logout token must not contain 'nonce' claim");
                return false;
            }
            return true;
        }

        private TenantConfigContext getTenantConfigContext(RoutingContext context) {
            String requestPath = context.request().path();
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
            return tenant.oidcConfig.isTenantEnabled()
                    && tenant.oidcConfig.getTenantId().get().equals(oidcTenantConfig.getTenantId().get())
                    && requestPath.equals(tenant.oidcConfig.logout.backchannel.path.orElse(null));
        }
    }
}
