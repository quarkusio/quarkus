package io.quarkus.quickcli.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a positional parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parameters {

    /** The index of this positional parameter (e.g., "0", "1", "0..1", "0..*"). */
    String index() default "";

    /** Description shown in help output. */
    String description() default "";

    /** Default value as a string. */
    String defaultValue() default "";

    /**
     * Arity: how many values this parameter accepts.
     * Empty string means auto-detect from the field type.
     */
    String arity() default "";

    /** Parameter label shown in help output. */
    String paramLabel() default "";

    /** Whether this parameter is hidden from help output. */
    boolean hidden() default false;

    /** Regex to split multi-value parameters (e.g., "," for a,b,c). */
    String split() default "";
}
