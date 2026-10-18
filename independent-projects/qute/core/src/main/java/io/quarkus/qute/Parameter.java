package io.quarkus.qute;

import java.util.Objects;
import java.util.function.Predicate;

import io.quarkus.qute.SectionHelperFactory.ParametersInfo;

/**
 * Definition of a section factory parameter.
 *
 * @see ParametersInfo
 * @see SectionHelperFactory#getParameters()
 */
public final class Parameter {

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final String EMPTY = "$empty$";

    public final String name;

    public final String defaultValue;

    public final boolean optional;

    public final Predicate<String> valuePredicate;

    private static final Predicate<String> ALWAYS_TRUE = v -> true;
    private static final Predicate<String> ALWAYS_FALSE = v -> false;

    public Parameter(String name, String defaultValue, boolean optional) {
        this(name, defaultValue, optional, ALWAYS_TRUE);
    }

    private Parameter(String name, String defaultValue, boolean optional, Predicate<String> valuePredicate) {
        this.name = Objects.requireNonNull(name);
        this.defaultValue = defaultValue;
        this.optional = optional;
        this.valuePredicate = valuePredicate != null ? valuePredicate : ALWAYS_TRUE;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public boolean isOptional() {
        return optional;
    }

    /**
     * Allows a factory parameter to refuse a value of an <strong>unnamed</strong> actual parameter.
     *
     * @param value
     * @return {@code true} if the value is acceptable, {@code false} otherwise
     */
    public boolean accepts(String value) {
        return valuePredicate.test(value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Parameter [name=").append(name).append(", defaultValue=").append(defaultValue).append(", optional=")
                .append(optional).append("]");
        return builder.toString();
    }

    public static class Builder {

        private final String name;
        private String defaultValue;
        private boolean optional;
        private Predicate<String> valuePredicate;

        public Builder(String name) {
            this.name = name;
            this.optional = false;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder optional() {
            this.optional = true;
            return this;
        }

        public Builder valuePredicate(Predicate<String> valuePredicate) {
            this.valuePredicate = valuePredicate;
            return this;
        }

        public Builder ignoreUnnamedValues() {
            this.valuePredicate = ALWAYS_FALSE;
            return this;
        }

        public Parameter build() {
            return new Parameter(name, defaultValue, optional, valuePredicate);
        }

    }

}
