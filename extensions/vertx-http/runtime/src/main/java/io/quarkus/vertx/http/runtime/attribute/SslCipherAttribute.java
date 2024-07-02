package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLSession;

import io.vertx.ext.web.RoutingContext;

public class SslCipherAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final SslCipherAttribute INSTANCE = new SslCipherAttribute();

    private static final String NAME = "SSL Cipher";

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

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
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return SslCipherAttribute.NAME;
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
