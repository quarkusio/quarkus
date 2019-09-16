package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A marker indicating that the configuration item {@link ConfigItem} should be generated as a section.
 * The section will be generated only if the configuration item type is annotated with {@link ConfigGroup}
 */
@Documented
@Retention(SOURCE)
@Target({ FIELD, PARAMETER })
public @interface ConfigDocSection {
}
