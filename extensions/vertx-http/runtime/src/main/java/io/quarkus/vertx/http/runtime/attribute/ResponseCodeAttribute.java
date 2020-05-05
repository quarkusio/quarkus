package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * The request status code
 *
 */
public class ResponseCodeAttribute implements ExchangeAttribute {

    public static final String RESPONSE_CODE_SHORT = "%s";
    public static final String RESPONSE_CODE = "%{RESPONSE_CODE}";

    public static final ExchangeAttribute INSTANCE = new ResponseCodeAttribute();

    private ResponseCodeAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return Integer.toString(exchange.response().getStatusCode());
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        exchange.response().setStatusCode(Integer.parseInt(newValue));
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Response code";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(RESPONSE_CODE) || token.equals(RESPONSE_CODE_SHORT)) {
                return ResponseCodeAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
