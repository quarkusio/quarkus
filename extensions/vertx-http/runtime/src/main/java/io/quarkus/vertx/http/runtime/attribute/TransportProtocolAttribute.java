package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

public class TransportProtocolAttribute implements ExchangeAttribute {

    public static final String TRANSPORT_PROTOCOL = "%{TRANSPORT_PROTOCOL}";

    public static final ExchangeAttribute INSTANCE = new TransportProtocolAttribute();

    private TransportProtocolAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return exchange.request().scheme();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("transport getProtocol", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Transport Protocol";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(TRANSPORT_PROTOCOL)) {
                return TransportProtocolAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
