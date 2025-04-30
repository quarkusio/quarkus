package io.quarkus.vertx.http.runtime.attribute;

import java.util.concurrent.TimeUnit;

import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.ext.web.RoutingContext;

/**
 * The response time
 * <p>
 * This will only work if {@link VertxHttpConfig#recordRequestStartTime} has been set
 */
public class ResponseTimeAttribute implements ExchangeAttribute {

    private static final String FIRST_RESPONSE_TIME_NANOS = ResponseTimeAttribute.class.getName() + ".first-response-time";

    public static final String RESPONSE_TIME_MILLIS_SHORT = "%D";
    public static final String RESPONSE_TIME_SECONDS_SHORT = "%T";
    public static final String RESPONSE_TIME_MILLIS = "%{RESPONSE_TIME}";
    public static final String RESPONSE_TIME_MICROS = "%{RESPONSE_TIME_MICROS}";
    public static final String RESPONSE_TIME_NANOS = "%{RESPONSE_TIME_NANOS}";

    private final TimeUnit timeUnit;

    public ResponseTimeAttribute(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        Long requestStartTime = exchange.get(VertxHttpRecorder.REQUEST_START_TIME);
        if (requestStartTime == null) {
            return null;
        }
        final long nanos;
        Long first = exchange.get(FIRST_RESPONSE_TIME_NANOS);
        if (first != null) {
            nanos = first;
        } else {
            nanos = System.nanoTime() - requestStartTime;
            if (exchange.response().ended()) {
                //save the response time so it is consistent
                exchange.put(FIRST_RESPONSE_TIME_NANOS, nanos);
            }
        }
        if (timeUnit == TimeUnit.SECONDS) {
            StringBuilder buf = new StringBuilder();
            long millis = TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
            buf.append(Long.toString(millis / 1000));
            buf.append('.');
            int remains = (int) (millis % 1000);
            buf.append(Long.toString(remains / 100));
            remains = remains % 100;
            buf.append(Long.toString(remains / 10));
            buf.append(Long.toString(remains % 10));
            return buf.toString();
        } else {
            return String.valueOf(timeUnit.convert(nanos, TimeUnit.NANOSECONDS));
        }
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Response Time", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Response Time";
        }

        @Override
        public ExchangeAttribute build(String token) {
            if (token.equals(RESPONSE_TIME_MILLIS) || token.equals(RESPONSE_TIME_MILLIS_SHORT)) {
                return new ResponseTimeAttribute(TimeUnit.MILLISECONDS);
            }
            if (token.equals(RESPONSE_TIME_SECONDS_SHORT)) {
                return new ResponseTimeAttribute(TimeUnit.SECONDS);
            }
            if (token.equals(RESPONSE_TIME_MICROS)) {
                return new ResponseTimeAttribute(TimeUnit.MICROSECONDS);
            }
            if (token.equals(RESPONSE_TIME_NANOS)) {
                return new ResponseTimeAttribute(TimeUnit.NANOSECONDS);
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }

}
