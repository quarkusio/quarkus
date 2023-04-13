package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

public class TransportProtocolAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String TRANSPORT_PROTOCOL = "%{TRANSPORT_PROTOCOL}";

    public static final ExchangeAttribute INSTANCE = new TransportProtocolAttribute();

    private static final String NAME = "Transport Protocol";

    private TransportProtocolAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return exchange.request().scheme();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return TransportProtocolAttribute.NAME;
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
