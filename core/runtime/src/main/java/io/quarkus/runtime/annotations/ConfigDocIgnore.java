package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A marker indicating that the configuration method should be ignored
 * when generating documentation.
 */
@Documented
@Retention(SOURCE)
@Target({ METHOD })
public @interface ConfigDocIgnore {
}
