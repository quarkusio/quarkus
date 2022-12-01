package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.ext.web.RoutingContext;

/**
 * A cookie
 *
 */
public class CookieAttribute implements ExchangeAttribute {

    private final String cookieName;

    public CookieAttribute(final String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        Cookie cookie = exchange.getCookie(cookieName);
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        exchange.response().addCookie(new CookieImpl(cookieName, newValue));
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Cookie";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.startsWith("%{c,") && token.endsWith("}")) {
                final String cookieName = token.substring(4, token.length() - 1);
                return new CookieAttribute(cookieName);
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
