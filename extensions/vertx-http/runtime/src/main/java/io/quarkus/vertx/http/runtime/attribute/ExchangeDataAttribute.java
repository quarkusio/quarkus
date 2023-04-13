package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * Provide entries from the "user data" section of the RoutingContext
 */
public class ExchangeDataAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    private static final String NAME = "Exchange data";

    private final String dataKey;

    public ExchangeDataAttribute(String dataKey) {
        this.dataKey = dataKey;
    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        return exchange.get(dataKey);
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return ExchangeDataAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.startsWith("%{d,") && token.endsWith("}")) {
                final String dataItemName = token.substring(4, token.length() - 1);
                return new ExchangeDataAttribute(dataItemName);
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }

}
