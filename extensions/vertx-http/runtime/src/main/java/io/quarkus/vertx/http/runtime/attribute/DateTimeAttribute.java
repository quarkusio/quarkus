package io.quarkus.vertx.http.runtime.attribute;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.vertx.ext.web.RoutingContext;

/**
 * The current time
 *
 */
public class DateTimeAttribute implements ExchangeAttribute {

    private static final String COMMON_LOG_PATTERN = "[dd/MMM/yyyy:HH:mm:ss Z]";

    public static final String DATE_TIME_SHORT = "%t";
    public static final String DATE_TIME = "%{DATE_TIME}";
    public static final String CUSTOM_TIME = "%{time,";

    public static final ExchangeAttribute INSTANCE = new DateTimeAttribute();

    private final DateTimeFormatter formatter;

    private DateTimeAttribute() {
        this(COMMON_LOG_PATTERN, null);
    }

    public DateTimeAttribute(final String dateFormat) {
        this(dateFormat, null);
    }

    public DateTimeAttribute(final String dateFormat, final String timezone) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat, Locale.US);
        if (timezone != null) {
            fmt = fmt.withZone(ZoneId.of(timezone));
        }
        this.formatter = fmt;
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return formatter.format(LocalDateTime.now());
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Date time", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "Date Time";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals(DATE_TIME) || token.equals(DATE_TIME_SHORT)) {
                return DateTimeAttribute.INSTANCE;
            }
            if (token.startsWith(CUSTOM_TIME) && token.endsWith("}")) {
                return new DateTimeAttribute(token.substring(CUSTOM_TIME.length(), token.length() - 1));
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
