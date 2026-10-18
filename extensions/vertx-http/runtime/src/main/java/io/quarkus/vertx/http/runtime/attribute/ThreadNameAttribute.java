package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * The thread name
 *
 */
public class ThreadNameAttribute implements ExchangeAttribute {

    public static final String THREAD_NAME_SHORT = "%I";
    public static final String THREAD_NAME = "%{THREAD_NAME}";

    public static final ExchangeAttribute INSTANCE = new ThreadNameAttribute();

    private ThreadNameAttribute() {

    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return Thread.currentThread().getName();
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Thread name", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Thread name";
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
