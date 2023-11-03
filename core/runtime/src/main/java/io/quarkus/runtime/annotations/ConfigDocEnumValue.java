package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A way to explicitly customize the string displayed in the documentation
 * when listing accepted values for an enum.
 * <p>
 * Only works when applied to enum values.
 */
@Retention(RUNTIME)
@Target({ FIELD })
@Documented
public @interface ConfigDocEnumValue {

    /**
     * @return The string displayed in the documentation for this value
     *         when listing accepted values for a configuration property of the relevant enum type.
     */
    String value();

}
