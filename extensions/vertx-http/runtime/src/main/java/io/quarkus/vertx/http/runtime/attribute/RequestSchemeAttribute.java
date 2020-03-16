package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * The request scheme
 *
 */
public class RequestSchemeAttribute implements ExchangeAttribute {

    public static final String REQUEST_SCHEME = "%{SCHEME}";

    public static final ExchangeAttribute INSTANCE = new RequestSchemeAttribute();

    private RequestSchemeAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return exchange.request().scheme();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request scheme";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_SCHEME)) {
                return RequestSchemeAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
