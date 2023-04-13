package io.quarkus.vertx.http.runtime.attribute;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * The current time
 *
 */
public class DateTimeAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    private static final String COMMON_LOG_PATTERN = "[dd/MMM/yyyy:HH:mm:ss Z]";

    public static final String DATE_TIME_SHORT = "%t";
    public static final String DATE_TIME = "%{DATE_TIME}";
    public static final String CUSTOM_TIME = "%{time,";

    public static final ExchangeAttribute INSTANCE = new DateTimeAttribute();

    private static final String NAME = "Date Time";

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
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        return formatter.format(ZonedDateTime.now());
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException(NAME, newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return DateTimeAttribute.NAME;
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
