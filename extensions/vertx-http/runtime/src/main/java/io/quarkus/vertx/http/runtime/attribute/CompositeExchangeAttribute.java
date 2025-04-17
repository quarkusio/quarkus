package io.quarkus.vertx.http.runtime.attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * Exchange attribute that represents a combination of attributes that should be merged into a single string.
 *
 */
public class CompositeExchangeAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    private final ExchangeAttribute[] attributes;

    public CompositeExchangeAttribute(ExchangeAttribute[] attributes) {
        ExchangeAttribute[] copy = new ExchangeAttribute[attributes.length];
        System.arraycopy(attributes, 0, copy, 0, attributes.length);
        this.attributes = copy;
    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        final Map<String, Optional<String>> serialized = new HashMap<>();
        for (int i = 0; i < attributes.length; ++i) {
            serialized.putAll(((ExchangeAttributeSerializable) attributes[i]).serialize(exchange));
        }
        return serialized;
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attributes.length; ++i) {
            final String val = attributes[i].readAttribute(exchange);
            if (val != null) {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("combined", newValue);
    }
}
