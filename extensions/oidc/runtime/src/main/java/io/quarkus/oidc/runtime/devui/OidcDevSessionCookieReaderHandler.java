package io.quarkus.oidc.runtime.devui;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.DefaultTokenStateManager;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

final class OidcDevSessionCookieReaderHandler implements Handler<RoutingContext> {

    private final OidcTenantConfig defaultTenantConfig;

    OidcDevSessionCookieReaderHandler(OidcConfig oidcConfig) {
        this.defaultTenantConfig = OidcTenantConfig.of(OidcConfig.getDefaultTenant(oidcConfig));
    }

    @Override
    public void handle(RoutingContext rc) {
        Cookie cookie = rc.request().getCookie(OidcUtils.SESSION_COOKIE_NAME);
        if (cookie != null) {
            DefaultTokenStateManager tokenStateManager = Arc.container().instance(DefaultTokenStateManager.class).get();
            Uni<AuthorizationCodeTokens> tokensUni = tokenStateManager.getTokens(rc, defaultTenantConfig, cookie.getValue(),
                    null);
            tokensUni.subscribe().with(tokens -> {
                rc.response().setStatusCode(200);
                rc.response().putHeader("Content-Type", "application/json");
                rc.end("{\"id_token\": \"" + tokens.getIdToken() + "\", \"access_token\": \"" + tokens.getAccessToken()
                        + "\", \"refresh_token\": \""
                        + tokens.getRefreshToken()
                        + "\"}");
            }, rc::fail);
        } else {
            rc.response().setStatusCode(200);
            rc.response().putHeader("Content-Type", "application/json");
            // empty: not logged in
            rc.end("{}");
        }
    }
}
