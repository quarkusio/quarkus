package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

/**
 * The remote IP address
 *
 */
public class RemoteIPAttribute implements ExchangeAttribute {

    public static final String REMOTE_IP_SHORT = "%a";
    public static final String REMOTE_IP = "%{REMOTE_IP}";

    public static final ExchangeAttribute INSTANCE = new RemoteIPAttribute();

    private RemoteIPAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        final SocketAddress sourceAddress = exchange.request().remoteAddress();
        if (sourceAddress == null) {
            return null;
        }
        return sourceAddress.host();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Remote IP", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Remote IP";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REMOTE_IP) || token.equals(REMOTE_IP_SHORT)) {
                return RemoteIPAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
