package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Defines a {@link Converter} to be used for conversion of a config property.
 * This will override the default converter on the target config item.
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@Documented
public @interface ConvertWith {
    /**
     * Specify the relative name of the configuration item.
     *
     * @return the name
     */
    Class<? extends Converter<?>> value();
}
