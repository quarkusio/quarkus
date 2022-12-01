package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * Exchange attribute that wraps string attributes in quotes.
 *
 * This is mostly used
 *
 */
public class QuotingExchangeAttribute implements ExchangeAttribute {

    private final ExchangeAttribute exchangeAttribute;

    public static final ExchangeAttributeWrapper WRAPPER = new Wrapper();

    public QuotingExchangeAttribute(ExchangeAttribute exchangeAttribute) {
        this.exchangeAttribute = exchangeAttribute;
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        String svalue = exchangeAttribute.readAttribute(exchange);
        // Does the value contain a " ? If so must encode it
        if (svalue == null || "-".equals(svalue) || svalue.isEmpty()) {
            return "-";
        }

        /* Wrap all quotes in double quotes. */
        StringBuilder buffer = new StringBuilder(svalue.length() + 2);
        buffer.append('\'');
        int i = 0;
        while (i < svalue.length()) {
            int j = svalue.indexOf('\'', i);
            if (j == -1) {
                buffer.append(svalue.substring(i));
                i = svalue.length();
            } else {
                buffer.append(svalue.substring(i, j + 1));
                buffer.append('"');
                i = j + 2;
            }
        }

        buffer.append('\'');
        return buffer.toString();
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static class Wrapper implements ExchangeAttributeWrapper {

        @Override
        public ExchangeAttribute wrap(final ExchangeAttribute attribute) {
            return new QuotingExchangeAttribute(attribute);
        }
    }
}
