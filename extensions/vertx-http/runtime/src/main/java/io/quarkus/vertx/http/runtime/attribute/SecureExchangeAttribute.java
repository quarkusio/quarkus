package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

public class SecureExchangeAttribute implements ExchangeAttribute {

    public static final String TOKEN = "%{SECURE}";

    public static final String LEGACY_INCORRECT_TOKEN = "${SECURE}"; //this was a bug, but we still support it for compat
    public static final ExchangeAttribute INSTANCE = new SecureExchangeAttribute();

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
            return "Secure";
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
