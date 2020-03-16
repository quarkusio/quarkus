package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * Exchange attribute that represents a fixed value
 *
 */
public class ConstantExchangeAttribute implements ExchangeAttribute {

    private final String value;

    public ConstantExchangeAttribute(final String value) {
        this.value = value;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return value;
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("constant", newValue);
    }
}
