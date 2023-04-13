package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * The request scheme
 *
 */
public class RequestSchemeAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String REQUEST_SCHEME = "%{SCHEME}";

    public static final ExchangeAttribute INSTANCE = new RequestSchemeAttribute();

    private static final String NAME = "Request scheme";

    private RequestSchemeAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return exchange.request().scheme();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return RequestSchemeAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_SCHEME)) {
                return RequestSchemeAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
