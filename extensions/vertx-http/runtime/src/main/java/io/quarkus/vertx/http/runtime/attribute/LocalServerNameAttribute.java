package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * The local server name
 *
 */
public class LocalServerNameAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String LOCAL_SERVER_NAME_SHORT = "%v";
    public static final String LOCAL_SERVER_NAME = "%{LOCAL_SERVER_NAME}";

    public static final ExchangeAttribute INSTANCE = new LocalServerNameAttribute();

    private static final String NAME = "Local server name";

    private LocalServerNameAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return exchange.request().host();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return LocalServerNameAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(LOCAL_SERVER_NAME) || token.equals(LOCAL_SERVER_NAME_SHORT)) {
                return LocalServerNameAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
