package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * Whether the response was completed before the request was done: {@code X} when the client closed the connection
 * before the response was ended, {@code -} otherwise, like the {@code %X} directive of Apache httpd.
 */
public class ConnectionStatusAttribute implements ExchangeAttribute {

    public static final String CONNECTION_STATUS = "%{CONNECTION_STATUS}";

    public static final ExchangeAttribute INSTANCE = new ConnectionStatusAttribute();

    private ConnectionStatusAttribute() {
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        return exchange.response().ended() ? "-" : "X";
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Connection status", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Connection status";
        }

        @Override
        public ExchangeAttribute build(String token) {
            if (token.equals(CONNECTION_STATUS)) {
                return ConnectionStatusAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
