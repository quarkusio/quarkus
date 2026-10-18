package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

/**
 * The local port
 *
 */
public class LocalPortAttribute implements ExchangeAttribute {

    public static final String LOCAL_PORT_SHORT = "%p";
    public static final String LOCAL_PORT = "%{LOCAL_PORT}";

    public static final ExchangeAttribute INSTANCE = new LocalPortAttribute();

    private LocalPortAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        final SocketAddress localAddr = exchange.request().localAddress();
        if (localAddr == null) {
            return null;
        }
        return Integer.toString(localAddr.port());
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Local port", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Local Port";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(LOCAL_PORT) || token.equals(LOCAL_PORT_SHORT)) {
                return LocalPortAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
