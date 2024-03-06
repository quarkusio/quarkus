package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

public class RequestPathAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String REQUEST_PATH = "%{REQUEST_PATH}";
    public static final String REQUEST_PATH_SHORT = "%R";

    public static final ExchangeAttribute INSTANCE = new RequestPathAttribute();

    private static final String NAME = "Request Path";

    private RequestPathAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return exchange.request().path();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return RequestPathAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            return token.equals(REQUEST_PATH) || token.equals(REQUEST_PATH_SHORT) ? INSTANCE : null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
