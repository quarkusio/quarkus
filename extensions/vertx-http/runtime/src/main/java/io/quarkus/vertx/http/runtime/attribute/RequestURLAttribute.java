package io.quarkus.vertx.http.runtime.attribute;

import io.quarkus.vertx.http.runtime.filters.OriginalRequestContext;
import io.vertx.ext.web.RoutingContext;

/**
 * The request URL
 *
 */
public class RequestURLAttribute implements ExchangeAttribute {

    public static final String REQUEST_URL_SHORT = "%U";
    public static final String REQUEST_URL = "%{REQUEST_URL}";
    public static final String ORIGINAL_REQUEST_URL_SHORT = "%<U";
    public static final String ORIGINAL_REQUEST_URL = "%{<REQUEST_URL}";

    public static final ExchangeAttribute INSTANCE = new RequestURLAttribute(false);
    public static final ExchangeAttribute INSTANCE_ORIGINAL_REQUEST = new RequestURLAttribute(true);

    private final boolean useOriginalRequest;

    private RequestURLAttribute(boolean useOriginalRequest) {
        this.useOriginalRequest = useOriginalRequest;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return useOriginalRequest ? OriginalRequestContext.getUri(exchange) : exchange.request().uri();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request URL";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_URL) || token.equals(REQUEST_URL_SHORT)) {
                return RequestURLAttribute.INSTANCE;
            } else if (token.equals(ORIGINAL_REQUEST_URL) || token.equals(ORIGINAL_REQUEST_URL_SHORT)) {
                return RequestURLAttribute.INSTANCE_ORIGINAL_REQUEST;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
