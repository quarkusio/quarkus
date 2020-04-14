package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * The query string
 *
 */
public class QueryStringAttribute implements ExchangeAttribute {

    public static final String QUERY_STRING_SHORT = "%q";
    public static final String QUERY_STRING = "%{QUERY_STRING}";
    public static final String BARE_QUERY_STRING = "%{BARE_QUERY_STRING}";

    public static final ExchangeAttribute INSTANCE = new QueryStringAttribute(true);
    public static final ExchangeAttribute BARE_INSTANCE = new QueryStringAttribute(false);

    private final boolean includeQuestionMark;

    private QueryStringAttribute(boolean includeQuestionMark) {
        this.includeQuestionMark = includeQuestionMark;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        String qs = exchange.request().query();
        if (qs == null) {
            qs = "";
        }
        if (qs.isEmpty() || !includeQuestionMark) {
            return qs;
        }
        return '?' + qs;
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Query String";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(QUERY_STRING) || token.equals(QUERY_STRING_SHORT)) {
                return QueryStringAttribute.INSTANCE;
            } else if (token.equals(BARE_QUERY_STRING)) {
                return QueryStringAttribute.BARE_INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
