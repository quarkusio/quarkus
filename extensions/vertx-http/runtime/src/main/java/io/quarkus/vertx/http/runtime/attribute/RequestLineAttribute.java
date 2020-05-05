package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * The request line
 *
 */
public class RequestLineAttribute implements ExchangeAttribute {

    public static final String REQUEST_LINE_SHORT = "%r";
    public static final String REQUEST_LINE = "%{REQUEST_LINE}";

    public static final ExchangeAttribute INSTANCE = new RequestLineAttribute();

    private RequestLineAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        StringBuilder sb = new StringBuilder()
                .append(exchange.request().method())
                .append(' ')
                .append(exchange.request().uri());
        if (exchange.request().query() != null) {
            sb.append('?');
            sb.append(exchange.request().query());
        }
        sb.append(' ')
                .append(exchange.request().version());
        return sb.toString();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Request line", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request line";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_LINE) || token.equals(REQUEST_LINE_SHORT)) {
                return RequestLineAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
