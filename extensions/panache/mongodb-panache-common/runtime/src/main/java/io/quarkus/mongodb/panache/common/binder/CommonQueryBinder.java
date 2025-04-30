package io.quarkus.mongodb.panache.common.binder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;

final class CommonQueryBinder {

    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    // the default DateTimeFormatter.ISO_LOCAL_DATE_TIME format with too many nano fraction (up to 9) for MongoDB (only 3)
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_PATTERN);

    private CommonQueryBinder() {
    }

    static String replace(String query, String oldChars, Object value) {
        return query.replace(oldChars, escape(value));
    }

    static String escape(Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isArray() || Collection.class.isAssignableFrom(value.getClass())) {
            return "[" + arrayAsString(value) + "]";
        }
        if (Number.class.isAssignableFrom(value.getClass()) || value instanceof Boolean) {
            return value.toString();
        }
        if (value.getClass().isEnum()) {
            return "'" + ((Enum<?>) value).name() + "'";
        }
        if (value instanceof Date) {
            ZonedDateTime zonedDateTime = ((Date) value).toInstant().atZone(ZoneOffset.UTC);
            return "{\"$date\": \"" + ISO_DATE_FORMATTER.format(zonedDateTime) + "\"} ";
        }
        if (value instanceof LocalDate) {
            ZonedDateTime zonedDateTime = ((LocalDate) value).atStartOfDay(ZoneOffset.UTC);
            return "{\"$date\": \"" + ISO_DATE_FORMATTER.format(zonedDateTime) + "\"} ";
        }
        if (value instanceof LocalDateTime) {
            ZonedDateTime zonedDateTime = ((LocalDateTime) value).atZone(ZoneOffset.UTC);
            return "{\"$date\": \"" + ISO_DATE_FORMATTER.format(zonedDateTime) + "\"} ";
        }
        if (value instanceof Instant) {
            ZonedDateTime zonedDateTime = ((Instant) value).atZone(ZoneOffset.UTC);
            return "{\"$date\": \"" + ISO_DATE_FORMATTER.format(zonedDateTime) + "\"} ";
        }
        if (value instanceof UUID) {
            return "UUID('" + value.toString() + "')";
        }
        if (value instanceof ObjectId) {
            ObjectId objectId = (ObjectId) value;
            return "ObjectId('" + objectId.toHexString() + "')";
        }
        return "'" + value.toString().replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /**
     * Converts Collection or Array to a String separated for ','. Used in $in operators
     */
    private static String arrayAsString(Object value) {
        Object[] valueArray = convertToArray(value);

        return Arrays.stream(valueArray)
                .map(CommonQueryBinder::escape)
                .collect(Collectors.joining(", "));
    }

    private static Object[] convertToArray(Object value) {
        if (value.getClass().isArray()) {
            return (Object[]) value;
        }

        Collection collection = (Collection) value;
        return collection.toArray(new Object[collection.size()]);
    }
}
