package io.quarkus.oidc.runtime.devui;

import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.RoutingContext;

final class OidcDevSessionLogoutHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext rc) {
        String redirect = rc.request().getParam("redirect_uri");
        ServerCookie cookie = (ServerCookie) rc.request().getCookie(OidcUtils.SESSION_COOKIE_NAME);
        if (cookie != null) {
            cookie.setValue("");
            cookie.setMaxAge(0L);
            cookie.setPath("/");
        }
        rc.response().setStatusCode(302);
        rc.response().putHeader("Location", redirect);
        rc.response().end();
    }
}
