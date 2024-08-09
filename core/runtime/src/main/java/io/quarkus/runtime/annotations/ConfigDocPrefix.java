package io.quarkus.runtime.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used when you want to override the top level prefix from the ConfigRoot/ConfigMapping for doc
 * generation.
 * <p>
 * This is for instance useful for {@code ConfigConfig}, which is an odd beast.
 * <p>
 * Should be considered very last resort.
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.TYPE })
public @interface ConfigDocPrefix {

    String value();
}
