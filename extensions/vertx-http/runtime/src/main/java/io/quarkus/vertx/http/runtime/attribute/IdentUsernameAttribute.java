package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * The ident username, not used, included for apache access log compatibility
 *
 */
public class IdentUsernameAttribute implements ExchangeAttribute {

    public static final String IDENT_USERNAME = "%l";

    public static final ExchangeAttribute INSTANCE = new IdentUsernameAttribute();

    private IdentUsernameAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return null;
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Ident username", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Ident Username";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(IDENT_USERNAME)) {
                return IdentUsernameAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
