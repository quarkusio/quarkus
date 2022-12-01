package io.quarkus.vertx.http.runtime.attribute;

import io.vertx.ext.web.RoutingContext;

/**
 * The bytes sent
 *
 */
public class BytesSentAttribute implements ExchangeAttribute {

    public static final String BYTES_SENT_SHORT_UPPER = "%B";
    public static final String BYTES_SENT_SHORT_LOWER = "%b";
    public static final String BYTES_SENT = "%{BYTES_SENT}";

    private final boolean dashIfZero;

    public BytesSentAttribute(boolean dashIfZero) {
        this.dashIfZero = dashIfZero;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        if (dashIfZero) {
            long bytesSent = exchange.response().bytesWritten();
            return bytesSent == 0 ? "-" : Long.toString(bytesSent);
        } else {
            return Long.toString(exchange.response().bytesWritten());
        }
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Bytes sent", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Bytes Sent";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(BYTES_SENT_SHORT_LOWER)) {
                return new BytesSentAttribute(true);
            }
            if (token.equals(BYTES_SENT) || token.equals(BYTES_SENT_SHORT_UPPER)) {
                return new BytesSentAttribute(false);
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
