package io.quarkus.oidc.runtime.dev.ui;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.DefaultTokenStateManager;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

final class OidcDevSessionCookieReaderHandler implements Handler<RoutingContext> {

    OidcDevSessionCookieReaderHandler() {

    }

    @Override
    public void handle(RoutingContext rc) {
        Cookie cookie = rc.request().getCookie(OidcUtils.SESSION_COOKIE_NAME);
        if (cookie != null) {
            DefaultTokenStateManager tokenStateManager = Arc.container().instance(DefaultTokenStateManager.class).get();
            OidcTenantConfig defaultTenantConfig = getDefaultTenantConfig();
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

    private static OidcTenantConfig getDefaultTenantConfig() {
        TenantConfigBean tenantConfigBean = Arc.container().instance(TenantConfigBean.class).get();
        if (tenantConfigBean.getDefaultTenant() != null && tenantConfigBean.getDefaultTenant().oidcConfig() != null) {
            return tenantConfigBean.getDefaultTenant().oidcConfig();
        }
        return OidcTenantConfig.builder().build();
    }
}
