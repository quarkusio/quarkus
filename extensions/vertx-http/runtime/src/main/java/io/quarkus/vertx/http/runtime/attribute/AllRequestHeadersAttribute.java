package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class AllRequestHeadersAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final AllRequestHeadersAttribute INSTANCE = new AllRequestHeadersAttribute();

    private static final String NAME = "Headers";

    private AllRequestHeadersAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        final MultiMap headers = exchange.request().headers();

        if (headers.isEmpty()) {
            return null;
        } else {
            final StringJoiner joiner = new StringJoiner(System.lineSeparator());

            for (Map.Entry<String, String> header : headers) {
                joiner.add(header.getKey() + ": " + header.getValue());
            }

            return joiner.toString();
        }
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return AllRequestHeadersAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals("%{ALL_REQUEST_HEADERS}")) {
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
