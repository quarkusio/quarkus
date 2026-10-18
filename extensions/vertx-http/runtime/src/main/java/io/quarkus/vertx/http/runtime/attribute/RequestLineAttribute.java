package io.quarkus.vertx.http.runtime.attribute;

import io.quarkus.vertx.http.runtime.filters.OriginalRequestContext;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * The request line
 *
 */
public class RequestLineAttribute implements ExchangeAttribute {

    public static final String REQUEST_LINE_SHORT = "%r";
    public static final String REQUEST_LINE = "%{REQUEST_LINE}";
    public static final String ORIGINAL_REQUEST_LINE_SHORT = "%<r";
    public static final String ORIGINAL_REQUEST_LINE = "%{<REQUEST_LINE}";

    public static final ExchangeAttribute INSTANCE = new RequestLineAttribute(false);
    public static final ExchangeAttribute INSTANCE_ORIGINAL_REQUEST = new RequestLineAttribute(true);

    private final boolean useOriginalRequest;

    private RequestLineAttribute(boolean useOriginalRequest) {
        this.useOriginalRequest = useOriginalRequest;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        HttpMethod httpMethod;
        String uri;
        if (useOriginalRequest) {
            if (!OriginalRequestContext.isPresent(exchange)) {
                return null;
            }
            httpMethod = OriginalRequestContext.getMethod(exchange);
            uri = OriginalRequestContext.getUri(exchange);
        } else {
            httpMethod = exchange.request().method();
            uri = exchange.request().uri();
        }

        return httpMethod + " " + uri + " " + RequestProtocolAttribute.getHttpVersionStr(exchange.request().version());
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Request line", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request line";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_LINE) || token.equals(REQUEST_LINE_SHORT)) {
                return RequestLineAttribute.INSTANCE;
            } else if (token.equals(ORIGINAL_REQUEST_LINE) || token.equals(ORIGINAL_REQUEST_LINE_SHORT)) {
                return RequestLineAttribute.INSTANCE_ORIGINAL_REQUEST;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
