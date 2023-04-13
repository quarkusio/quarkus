package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

/**
 * The local port
 *
 */
public class LocalPortAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String LOCAL_PORT_SHORT = "%p";
    public static final String LOCAL_PORT = "%{LOCAL_PORT}";

    public static final ExchangeAttribute INSTANCE = new LocalPortAttribute();

    private static final String NAME = "Local Port";

    private LocalPortAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
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
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return LocalPortAttribute.NAME;
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
