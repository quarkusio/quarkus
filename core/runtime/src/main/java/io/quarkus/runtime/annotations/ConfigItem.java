package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * A single container configuration item.
 */
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER })
@Documented
public @interface ConfigItem {

    /**
     * Constant value for {@link #name} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be de-camel-cased and
     * hyphenated, and then used.
     */
    String HYPHENATED_ELEMENT_NAME = "<<hyphenated element name>>";

    /**
     * Constant value for {@link #name()} indicating that the parent's name for the member referencing this item's
     * group should be used as the name of this item. This value is only valid for members of configuration groups.
     */
    String PARENT = "<<parent>>";

    /**
     * Constant value for {@link #defaultValue()} indicating that no default value should be used (the value is
     * a configuration group or it is {@link Optional}).
     */
    String NO_DEFAULT = "<<no default>>";

    /**
     * Specify the relative name of the configuration item.
     *
     * @return the name
     */
    String name() default HYPHENATED_ELEMENT_NAME;

    /**
     * Specify the default value of the configuration item, if none is found in the configuration.
     *
     * @return the default value
     */
    String defaultValue() default NO_DEFAULT;
}
