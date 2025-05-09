package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

/**
 * The remote Host address (if resolved)
 *
 */
public class RemoteHostAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String REMOTE_HOST_NAME_SHORT = "%h";
    public static final String REMOTE_HOST = "%{REMOTE_HOST}";

    public static final ExchangeAttribute INSTANCE = new RemoteHostAttribute();

    private static final String NAME = "Remote host";

    private RemoteHostAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        final SocketAddress remoteAddr = exchange.request().remoteAddress();
        if (remoteAddr == null) {
            return null;
        }
        return remoteAddr.host();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return RemoteHostAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REMOTE_HOST) || token.equals(REMOTE_HOST_NAME_SHORT)) {
                return RemoteHostAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
