package io.quarkus.oidc.runtime.dev.ui;

import static io.quarkus.oidc.runtime.OidcHelper.getSessionCookieName;

import io.vertx.core.Handler;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.RoutingContext;

final class OidcDevSessionLogoutHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext rc) {
        String redirect = rc.request().getParam("redirect_uri");
        ServerCookie cookie = (ServerCookie) rc.request().getCookie(getSessionCookieName());
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
