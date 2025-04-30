package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A marker indicating that the configuration item {@link ConfigItem} should be generated as a section.
 * The section will be generated only if the configuration item type is annotated with {@link ConfigGroup}
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
public @interface ConfigDocSection {

    /**
     * If we should generate a specific file for this section.
     * <p>
     * We used to do it for all config groups before but it's counterproductive.
     * The new annotation processor only generates a file for a config group
     * if this is true.
     */
    boolean generated() default false;
}
