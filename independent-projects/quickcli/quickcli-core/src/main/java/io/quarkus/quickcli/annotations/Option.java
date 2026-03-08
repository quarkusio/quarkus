package io.quarkus.quickcli.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.quickcli.ScopeType;

/**
 * Marks a field as a command-line option (e.g., --name, -n).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Option {

    /** Option names, e.g., {"-n", "--name"}. At least one name is required. */
    String[] names();

    /** Description shown in help output. */
    String[] description() default {};

    /** Whether this option is required. */
    boolean required() default false;

    /** Default value as a string. Empty string means no default. */
    String defaultValue() default "";

    /**
     * Arity: how many values this option accepts.
     * Examples: "0" (flag), "1" (single value), "0..1" (optional), "1..*" (one or more).
     * Empty string means auto-detect from the field type.
     */
    String arity() default "";

    /** If true, this option is hidden from help output. */
    boolean hidden() default false;

    /** Parameter label shown in help, e.g., FILE, HOST. */
    String paramLabel() default "";

    /** Whether this is a negatable boolean option. */
    boolean negatable() default false;

    /** Whether this option triggers usage help display (like --help). */
    boolean usageHelp() default false;

    /** Whether this option triggers version display (like --version). */
    boolean versionHelp() default false;

    /** Display order in help output. Lower values are shown first. */
    int order() default -1;

    /** Regex to split multi-value options (e.g., "," for --names=a,b,c). */
    String split() default "";

    /** Scope type. INHERIT means this option is inherited by subcommands. */
    ScopeType scope() default ScopeType.LOCAL;

    /** Fallback value for map-type options when key is specified without a value. */
    String mapFallbackValue() default "";

    /** Class that provides completion candidates for this option. */
    Class<? extends Iterable<String>> completionCandidates() default NullCompletionCandidates.class;
}
