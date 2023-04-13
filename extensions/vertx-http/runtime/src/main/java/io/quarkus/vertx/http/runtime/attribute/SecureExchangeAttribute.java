package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

public class SecureExchangeAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String TOKEN = "%{SECURE}";

    public static final String LEGACY_INCORRECT_TOKEN = "${SECURE}"; //this was a bug, but we still support it for compat
    public static final ExchangeAttribute INSTANCE = new SecureExchangeAttribute();

    private static final String NAME = "Secure";

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        return Boolean.toString(exchange.request().scheme().equals("https"));
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return SecureExchangeAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(String token) {
            if (token.equals(TOKEN) || token.equals(LEGACY_INCORRECT_TOKEN)) {
                return INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
