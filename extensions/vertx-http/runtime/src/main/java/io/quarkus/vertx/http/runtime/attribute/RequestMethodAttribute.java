package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.quarkus.vertx.http.runtime.filters.OriginalRequestContext;
import io.vertx.ext.web.RoutingContext;

/**
 * The request method
 *
 */
public class RequestMethodAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String REQUEST_METHOD_SHORT = "%m";
    public static final String REQUEST_METHOD = "%{METHOD}";
    public static final String ORIGINAL_REQUEST_METHOD_SHORT = "%<m";
    public static final String ORIGINAL_REQUEST_METHOD = "%{<METHOD}";

    public static final ExchangeAttribute INSTANCE = new RequestMethodAttribute(false);
    public static final ExchangeAttribute INSTANCE_ORIGINAL_REQUEST = new RequestMethodAttribute(true);

    private static final String NAME = "Request method";

    private final boolean useOriginalRequest;

    private RequestMethodAttribute(boolean useOriginalRequest) {
        this.useOriginalRequest = useOriginalRequest;
    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return useOriginalRequest ? OriginalRequestContext.getMethod(exchange).name() : exchange.request().method().name();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return RequestMethodAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(REQUEST_METHOD) || token.equals(REQUEST_METHOD_SHORT)) {
                return RequestMethodAttribute.INSTANCE;
            } else if (token.equals(ORIGINAL_REQUEST_METHOD) || token.equals(ORIGINAL_REQUEST_METHOD_SHORT)) {
                return RequestMethodAttribute.INSTANCE_ORIGINAL_REQUEST;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
