package io.quarkus.vertx.http.runtime.attribute;

import java.util.List;

import io.vertx.ext.web.RoutingContext;

/**
 * A request header
 *
 */
public class RequestHeaderAttribute implements ExchangeAttribute {

    private final String requestHeader;

    public RequestHeaderAttribute(final String requestHeader) {
        this.requestHeader = requestHeader;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        List<String> header = exchange.request().headers().getAll(requestHeader);
        if (header.isEmpty()) {
            return null;
        } else if (header.size() == 1) {
            return header.get(0);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < header.size(); ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(header.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        if (newValue == null) {
            exchange.request().headers().remove(requestHeader);
        } else {
            exchange.request().headers().set(requestHeader, newValue);
        }
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request header";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.startsWith("%{i,") && token.endsWith("}")) {
                final String headerName = token.substring(4, token.length() - 1);
                return new RequestHeaderAttribute(headerName);
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
