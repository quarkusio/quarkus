package io.quarkus.arc.properties;

/**
 * Defines how the {@code stringValue} of a property condition annotation is matched against
 * the actual property value.
 */
public enum StringValueMatch {

    /**
     * Exact, case-sensitive string equality (the default).
     */
    EQ,

    /**
     * The {@code stringValue} is interpreted as a regular expression and the actual
     * property value is matched against it using {@link java.util.regex.Pattern#matches(String, CharSequence)}.
     * <p>
     * The pattern must match the entire property value (equivalent to {@code ^pattern$}).
     */
    REGEX;
}
