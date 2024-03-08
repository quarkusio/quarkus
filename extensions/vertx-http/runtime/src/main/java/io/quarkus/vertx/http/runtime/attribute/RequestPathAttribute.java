package io.quarkus.vertx.http.runtime.attribute;

import io.quarkus.vertx.http.runtime.filters.OriginalRequestContext;
import io.vertx.ext.web.RoutingContext;

public class RequestPathAttribute implements ExchangeAttribute {

    public static final String REQUEST_PATH = "%{REQUEST_PATH}";
    public static final String REQUEST_PATH_SHORT = "%R";
    public static final String ORIGINAL_REQUEST_PATH = "%{<REQUEST_PATH}";
    public static final String ORIGINAL_REQUEST_PATH_SHORT = "%<R";

    public static final ExchangeAttribute INSTANCE = new RequestPathAttribute(false);
    public static final ExchangeAttribute INSTANCE_ORIGINAL_REQUEST = new RequestPathAttribute(true);

    private final boolean useOriginalRequest;

    private RequestPathAttribute(boolean useOriginalRequest) {
        this.useOriginalRequest = useOriginalRequest;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return useOriginalRequest ? OriginalRequestContext.getPath(exchange) : exchange.request().path();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException();
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Request Path";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(ORIGINAL_REQUEST_PATH) || token.equals(ORIGINAL_REQUEST_PATH_SHORT)) {
                return INSTANCE_ORIGINAL_REQUEST;
            }
            return token.equals(REQUEST_PATH) || token.equals(REQUEST_PATH_SHORT) ? INSTANCE : null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
