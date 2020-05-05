package io.quarkus.vertx.http.runtime.attribute;

import javax.net.ssl.SSLSession;

import io.vertx.ext.web.RoutingContext;

public class SslCipherAttribute implements ExchangeAttribute {

    public static final SslCipherAttribute INSTANCE = new SslCipherAttribute();

    @Override
    public String readAttribute(RoutingContext exchange) {
        SSLSession ssl = exchange.request().sslSession();
        if (ssl == null) {
            return null;
        }
        return ssl.getCipherSuite();
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("SSL Cipher", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "SSL Cipher";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals("%{SSL_CIPHER}")) {
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
