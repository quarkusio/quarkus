package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

/**
 * The local IP address
 */
public class LocalIPAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String LOCAL_IP = "%{LOCAL_IP}";
    public static final String LOCAL_IP_SHORT = "%A";

    public static final ExchangeAttribute INSTANCE = new LocalIPAttribute();

    private static final String NAME = "Local IP";

    private LocalIPAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        SocketAddress localAddress = exchange.request().localAddress();
        if (localAddress == null) {
            return null;
        }
        return localAddress.host();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return LocalIPAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(LOCAL_IP) || token.equals(LOCAL_IP_SHORT)) {
                return LocalIPAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
