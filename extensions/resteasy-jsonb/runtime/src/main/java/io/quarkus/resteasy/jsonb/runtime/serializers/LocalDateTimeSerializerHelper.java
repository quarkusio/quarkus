package io.quarkus.resteasy.jsonb.runtime.serializers;

import static org.eclipse.yasson.internal.serializer.AbstractDateTimeSerializer.UTC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public final class LocalDateTimeSerializerHelper {

    private LocalDateTimeSerializerHelper() {
    }

    public static String defaultFormat(LocalDateTime localDateTime, Locale locale) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(locale).format(localDateTime);
    }

    public static String timeInMillisFormat(LocalDateTime localDateTime) {
        return String.valueOf(localDateTime.atZone(UTC).toInstant().toEpochMilli());
    }

    public static String customFormat(LocalDateTime localDateTime, String format, Locale locale) {
        return new DateTimeFormatterBuilder().appendPattern(format).toFormatter(locale).withZone(UTC).format(localDateTime);
    }
}
