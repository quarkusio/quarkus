package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * The request URL
 *
 */
public class RequestURLAttribute implements ExchangeAttribute {

    public static final String REQUEST_URL_SHORT = "%U";
    public static final String REQUEST_URL = "%{REQUEST_URL}";

    public static final ExchangeAttribute INSTANCE = new RequestURLAttribute();

    private RequestURLAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return exchange.request().uri();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request URL";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_URL) || token.equals(REQUEST_URL_SHORT)) {
                return RequestURLAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
