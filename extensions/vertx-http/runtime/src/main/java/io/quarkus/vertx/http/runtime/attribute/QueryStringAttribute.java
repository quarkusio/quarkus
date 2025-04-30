package io.quarkus.vertx.http.runtime.attribute;

import io.quarkus.vertx.http.runtime.filters.OriginalRequestContext;
import io.vertx.ext.web.RoutingContext;

/**
 * The query string
 *
 */
public class QueryStringAttribute implements ExchangeAttribute {

    public static final String QUERY_STRING_SHORT = "%q";
    public static final String QUERY_STRING = "%{QUERY_STRING}";
    public static final String BARE_QUERY_STRING = "%{BARE_QUERY_STRING}";
    public static final String ORIGINAL_QUERY_STRING_SHORT = "%<q";
    public static final String ORIGINAL_QUERY_STRING = "%{<QUERY_STRING}";
    public static final String ORIGINAL_BARE_QUERY_STRING = "%{<BARE_QUERY_STRING}";

    public static final ExchangeAttribute INSTANCE = new QueryStringAttribute(true, false);
    public static final ExchangeAttribute BARE_INSTANCE = new QueryStringAttribute(false, false);
    public static final ExchangeAttribute INSTANCE_ORIGINAL_REQUEST = new QueryStringAttribute(true, true);
    public static final ExchangeAttribute BARE_INSTANCE_ORIGINAL_REQUEST = new QueryStringAttribute(false, true);

    private final boolean includeQuestionMark;
    private final boolean useOriginalRequest;

    private QueryStringAttribute(boolean includeQuestionMark, boolean useOriginalRequest) {
        this.includeQuestionMark = includeQuestionMark;
        this.useOriginalRequest = useOriginalRequest;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        String qs = useOriginalRequest ? OriginalRequestContext.getQuery(exchange) : exchange.request().query();
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
            } else if (token.equals(ORIGINAL_QUERY_STRING) || token.equals(ORIGINAL_QUERY_STRING_SHORT)) {
                return QueryStringAttribute.INSTANCE_ORIGINAL_REQUEST;
            } else if (token.equals(ORIGINAL_BARE_QUERY_STRING)) {
                return QueryStringAttribute.BARE_INSTANCE_ORIGINAL_REQUEST;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
