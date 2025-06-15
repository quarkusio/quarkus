package org.jboss.resteasy.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * An annotation that can be used on a date JAX-RS Resource method parameter type in order to determine the format that
 * will be used to parse that type. Supported types are:
 * <ul>
 * <li>java.time.LocalDate
 * <li>java.time.LocalDateTime
 * <li>java.time.LocalTime
 * <li>java.time.OffsetDateTime
 * <li>java.time.OffsetTime
 * <li>java.time.ZonedDateTime
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DateFormat {

    /**
     * If set, this string will be used in order to build a {@link DateTimeFormatter} using
     * {@code DateTimeFormatter.ofPattern}. Subsequently, the built {@link DateTimeFormatter} will be used in order to
     * parse the input String into the desired type.
     */
    String pattern() default UNSET_PATTERN;

    /**
     * If set, the class will be used to provide a {@link DateTimeFormatter} that will then be used in order to parse
     * the input String into the desired type.
     */
    Class<? extends DateTimeFormatterProvider> dateTimeFormatterProvider() default DateTimeFormatterProvider.UnsetDateTimeFormatterProvider.class;

    String UNSET_PATTERN = "<<unset>>";

    interface DateTimeFormatterProvider extends Supplier<DateTimeFormatter> {

        class UnsetDateTimeFormatterProvider implements DateTimeFormatterProvider {

            @Override
            public DateTimeFormatter get() {
                throw new IllegalStateException("Should never be called");
            }
        }
    }

}
