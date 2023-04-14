package io.quarkus.observability.promql.client.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.Locale;
import java.util.function.Function;

/**
 * Used in conjunction with {@link jakarta.ws.rs.QueryParam} or {@link jakarta.ws.rs.FormParam} to
 * annotate parameters of type {@link Instant} to make the {@link InstantParamConverterProvider}
 * provide with the Instant converter of desired {@link InstantFormat.Kind kind}.
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SuppressWarnings("checkstyle:Indentation")
public @interface InstantFormat {

    Kind value() default Kind.ISO;

    /**
     * Enumeration of kinds of Instant formats.
     */
    enum Kind {
        ISO(Instant::parse, Instant::toString),
        EPOCH_SECONDS(
                string -> Instant.ofEpochMilli((long) (Double.parseDouble(string) * 1000d)),
                value -> String.format(Locale.ROOT, "%f", (double) value.toEpochMilli() / 1000d)),
        EPOCH_MILLIS(
                string -> Instant.ofEpochMilli(Long.parseLong(string)),
                value -> String.valueOf(value.toEpochMilli()));

        final Function<String, Instant> fromString;
        final Function<Instant, String> toString;

        Kind(Function<String, Instant> fromString, Function<Instant, String> toString) {
            this.fromString = fromString;
            this.toString = toString;
        }
    }
}
