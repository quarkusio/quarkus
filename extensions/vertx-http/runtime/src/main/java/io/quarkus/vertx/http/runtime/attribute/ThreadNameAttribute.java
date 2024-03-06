package io.quarkus.vertx.http.runtime.attribute;

import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * The thread name
 *
 */
public class ThreadNameAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    public static final String THREAD_NAME_SHORT = "%I";
    public static final String THREAD_NAME = "%{THREAD_NAME}";

    public static final ExchangeAttribute INSTANCE = new ThreadNameAttribute();

    private static final String NAME = "Thread name";

    private ThreadNameAttribute() {

    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return Thread.currentThread().getName();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return ThreadNameAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(THREAD_NAME) || token.equals(THREAD_NAME_SHORT)) {
                return ThreadNameAttribute.INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
