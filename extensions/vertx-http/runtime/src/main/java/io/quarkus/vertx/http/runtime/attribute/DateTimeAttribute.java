package io.quarkus.vertx.http.runtime.attribute;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.vertx.ext.web.RoutingContext;

/**
 * The current time
 *
 */
public class DateTimeAttribute implements ExchangeAttribute {

    private static final String COMMON_LOG_PATTERN = "[dd/MMM/yyyy:HH:mm:ss Z]";

    private static final ThreadLocal<SimpleDateFormat> COMMON_LOG_PATTERN_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat df = new SimpleDateFormat(COMMON_LOG_PATTERN, Locale.US);
            return df;
        }
    };

    public static final String DATE_TIME_SHORT = "%t";
    public static final String DATE_TIME = "%{DATE_TIME}";
    public static final String CUSTOM_TIME = "%{time,";

    public static final ExchangeAttribute INSTANCE = new DateTimeAttribute();

    private final String dateFormat;
    private final ThreadLocal<SimpleDateFormat> cachedFormat;

    private DateTimeAttribute() {
        this.dateFormat = null;
        this.cachedFormat = null;
    }

    public DateTimeAttribute(final String dateFormat) {
        this(dateFormat, null);
    }

    public DateTimeAttribute(final String dateFormat, final String timezone) {
        this.dateFormat = dateFormat;
        this.cachedFormat = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                final SimpleDateFormat format = new SimpleDateFormat(dateFormat);
                if (timezone != null) {
                    format.setTimeZone(TimeZone.getTimeZone(timezone));
                }
                return format;
            }
        };
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        if (dateFormat == null) {
            return COMMON_LOG_PATTERN_FORMAT.get().format(new Date());
        } else {
            final SimpleDateFormat dateFormat = this.cachedFormat.get();
            return dateFormat.format(new Date());
        }
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
