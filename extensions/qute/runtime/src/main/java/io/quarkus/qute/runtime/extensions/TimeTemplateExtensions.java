package io.quarkus.qute.runtime.extensions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.inject.Vetoed;

import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
@TemplateExtension
public class TimeTemplateExtensions {

    private static final Map<Key, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    public static void clearCache() {
        FORMATTER_CACHE.clear();
    }

    static String format(TemporalAccessor temporal, String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(new Key(pattern, null, null), TimeTemplateExtensions::formatterForKey)
                .format(temporal);
    }

    static String format(TemporalAccessor temporal, String pattern, Locale locale) {
        return FORMATTER_CACHE.computeIfAbsent(new Key(pattern, locale, null), TimeTemplateExtensions::formatterForKey)
                .format(temporal);
    }

    static String format(TemporalAccessor temporal, String pattern, Locale locale, ZoneId timeZone) {
        return FORMATTER_CACHE.computeIfAbsent(new Key(pattern, locale, timeZone), TimeTemplateExtensions::formatterForKey)
                .format(temporal);
    }

    @TemplateExtension(namespace = "time")
    static String format(Object dateTimeObject, String pattern) {
        return format(getFormattableObject(dateTimeObject, ZoneId.systemDefault()), pattern);
    }

    @TemplateExtension(namespace = "time")
    static String format(Object dateTimeObject, String pattern, Locale locale) {
        return format(getFormattableObject(dateTimeObject, ZoneId.systemDefault()), pattern, locale);
    }

    @TemplateExtension(namespace = "time")
    static String format(Object dateTimeObject, String pattern, Locale locale, ZoneId timeZone) {
        return format(getFormattableObject(dateTimeObject, timeZone), pattern, locale, timeZone);
    }

    private static TemporalAccessor getFormattableObject(Object value,
            ZoneId timeZone) {
        if (value instanceof TemporalAccessor) {
            return (TemporalAccessor) value;
        } else if (value instanceof Date) {
            return LocalDateTime.ofInstant(((Date) value).toInstant(),
                    timeZone);
        } else if (value instanceof Calendar) {
            return LocalDateTime.ofInstant(((Calendar) value).toInstant(),
                    timeZone);
        } else if (value instanceof Number) {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(((Number) value).longValue()),
                    timeZone);
        } else {
            throw new IllegalArgumentException("Not a formattable date/time object: " + value);
        }
    }

    private static DateTimeFormatter formatterForKey(Key key) {
        DateTimeFormatter formatter;
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder.appendPattern(key.pattern);
        if (key.locale != null) {
            formatter = builder.toFormatter(key.locale);
        } else {
            formatter = builder.toFormatter();
        }
        return key.timeZone != null ? formatter.withZone(key.timeZone) : formatter;
    }

    static final class Key {

        private final String pattern;
        private final Locale locale;
        private final ZoneId timeZone;
        private final int hashCode;

        public Key(String pattern, Locale locale, ZoneId timeZone) {
            this.pattern = pattern;
            this.locale = locale;
            this.timeZone = timeZone;
            final int prime = 31;
            int result = 1;
            result = prime * result + ((locale == null) ? 0 : locale.hashCode());
            result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
            result = prime * result + ((timeZone == null) ? 0 : timeZone.hashCode());
            this.hashCode = result;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            return Objects.equals(locale, other.locale) && Objects.equals(pattern, other.pattern)
                    && Objects.equals(timeZone, other.timeZone);
        }

    }

}
