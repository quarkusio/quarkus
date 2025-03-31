package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * The request status code
 *
 */
public class ResponseCodeAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String RESPONSE_CODE_SHORT = "%s";
    public static final String RESPONSE_CODE = "%{RESPONSE_CODE}";

    public static final ExchangeAttribute INSTANCE = new ResponseCodeAttribute();

    private static final String NAME = "Response code";

    private ResponseCodeAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return Integer.toString(exchange.response().getStatusCode());
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        exchange.response().setStatusCode(Integer.parseInt(newValue));
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return ResponseCodeAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(RESPONSE_CODE) || token.equals(RESPONSE_CODE_SHORT)) {
                return ResponseCodeAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
