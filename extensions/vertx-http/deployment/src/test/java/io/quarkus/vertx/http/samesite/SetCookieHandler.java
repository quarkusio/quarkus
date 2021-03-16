package io.quarkus.vertx.http.samesite;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.CookieImpl;

@ApplicationScoped
public class SetCookieHandler {

    public void handler(@Observes Router router) {
        router.route("/cookie").handler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                event.response().addCookie(new CookieImpl("cookie1", "value1"));
                event.response().addCookie(new CookieImpl("COOKIE2", "VALUE2"));
                event.response().addCookie(new CookieImpl("cookie3", "value3"));
                event.response().addCookie(new CookieImpl("COOKIE4", "VALUE4"));
                event.response().addCookie(new CookieImpl("foo", "foo"));
                event.response().end();
            }
        });
    }
}
