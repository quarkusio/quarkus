package io.quarkus.quickcli.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as an argument group, enabling mutually exclusive or co-occurring options.
 * The field type should be a class containing {@code @Option} and/or {@code @Parameters} fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ArgGroup {

    /** Whether the options in this group are mutually exclusive. */
    boolean exclusive() default true;

    /**
     * Multiplicity: how many times this group can/must occur.
     * Examples: "0..1" (optional), "1" (required exactly once), "0..*" (any number).
     */
    String multiplicity() default "0..1";

    /** Heading for this group in help output. */
    String heading() default "";

    /** Display order in help output. Lower values are shown first. */
    int order() default -1;

    /** Whether to validate group constraints. */
    boolean validate() default true;
}
