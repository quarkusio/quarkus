package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * The request getProtocol
 *
 */
public class RequestProtocolAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String REQUEST_PROTOCOL_SHORT = "%H";
    public static final String REQUEST_PROTOCOL = "%{PROTOCOL}";

    public static final ExchangeAttribute INSTANCE = new RequestProtocolAttribute();

    private static final String NAME = "Request Protocol";

    private RequestProtocolAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return exchange.request().version().name();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return RequestProtocolAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_PROTOCOL) || token.equals(REQUEST_PROTOCOL_SHORT)) {
                return RequestProtocolAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
