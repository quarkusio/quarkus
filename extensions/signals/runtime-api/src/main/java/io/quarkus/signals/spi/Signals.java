package io.quarkus.signals.spi;

import java.lang.annotation.Annotation;

import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.signals.Signal;
import io.smallrye.common.annotation.Experimental;

/**
 * SPI for programmatic creation of {@link Signal} instances.
 * <p>
 * Application code should prefer {@code @Inject Signal<T>} whenever possible and use
 * {@link Signal#create(Class, Annotation...)}
 * or {@link Signal#create(TypeLiteral, Annotation...)} when CDI injection is not available.
 * This SPI is intended for framework integrators.
 */
@Experimental("This API is experimental and may change in the future")
public interface Signals {

    /**
     * Creates a new {@link Signal} instance for the given signal type and qualifiers.
     *
     * @param <T> the type of the signal object
     * @param type the signal type
     * @param qualifiers the specified qualifiers
     * @return the {@code Signal}, never {@code null}
     */
    <T> Signal<T> create(Class<T> type, Annotation... qualifiers);

    /**
     * Creates a new {@link Signal} instance for the given signal type and qualifiers.
     *
     * @param <T> the type of the signal object
     * @param subtype a {@link TypeLiteral} representing the signal type
     * @param qualifiers the specified qualifiers
     * @return the {@code Signal}, never {@code null}
     */
    <T> Signal<T> create(TypeLiteral<T> subtype, Annotation... qualifiers);

}
