package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

public class NullAttribute implements ExchangeAttribute {

    public static final String NAME = "%{NULL}";

    public static final NullAttribute INSTANCE = new NullAttribute();

    private NullAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return null;
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "null";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(NAME)) {
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
