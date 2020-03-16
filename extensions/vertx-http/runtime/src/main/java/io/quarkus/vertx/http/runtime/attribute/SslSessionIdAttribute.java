package io.quarkus.vertx.http.runtime.attribute;

import java.util.Base64;

import javax.net.ssl.SSLSession;

import io.vertx.ext.web.RoutingContext;

public class SslSessionIdAttribute implements ExchangeAttribute {

    public static final SslSessionIdAttribute INSTANCE = new SslSessionIdAttribute();

    @Override
    public String readAttribute(RoutingContext exchange) {
        SSLSession ssl = exchange.request().sslSession();
        if (ssl == null || ssl.getId() == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(ssl.getId());
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("SSL Session ID", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "SSL Session ID";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals("%{SSL_SESSION_ID}")) {
                return INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
