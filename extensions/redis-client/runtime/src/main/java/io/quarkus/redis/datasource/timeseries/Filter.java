package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

/**
 * Represents a filter used in the {@code MGET} command.
 */
public interface Filter {

    String toString();

    /**
     * Creates a {@code label=value}, selecting samples where the label equals value.
     *
     * @param label
     *        the label, must not be {@code null}
     * @param value
     *        the value, must not be {@code null}
     *
     * @return the filter
     */
    static Filter withLabel(String label, Object value) {
        nonNull(label, "label");
        nonNull(value, "value");
        return new Filter() {
            @Override
            public String toString() {
                return label + "=" + value;
            }
        };
    }

    /**
     * Creates a {@code label!=value}, selecting samples where the label is not equal to value.
     *
     * @param label
     *        the label, must not be {@code null}
     * @param value
     *        the value, must not be {@code null}
     *
     * @return the filter
     */
    static Filter withoutLabel(String label, String value) {
        nonNull(label, "label");
        nonNull(value, "value");
        return new Filter() {
            @Override
            public String toString() {
                return label + "!=" + value;
            }
        };
    }

    /**
     * Creates a {@code label=}, selecting samples containing the given label.
     *
     * @param label
     *        the label, must not be {@code null}
     *
     * @return the filter
     */
    static Filter withLabel(String label) {
        nonNull(label, "label");
        return new Filter() {
            @Override
            public String toString() {
                return label + "=";
            }
        };
    }

    /**
     * Creates a {@code label!=}, selecting samples that do not have the given label.
     *
     * @param label
     *        the label, must not be {@code null}
     *
     * @return the filter
     */
    static Filter withoutLabel(String label) {
        nonNull(label, "label");
        return new Filter() {
            @Override
            public String toString() {
                return label + "!=";
            }
        };

    }

    /**
     * Creates a {@code label=(value1,value2,...)}, selecting samples with the given label equals one of the values in
     * the list
     *
     * @param label
     *        the label, must not be {@code null}
     *
     * @return the filter
     */
    static Filter withLabelHavingValueFrom(String label, String... values) {
        nonNull(label, "label");
        doesNotContainNull(values, "values");
        return new Filter() {
            @Override
            public String toString() {
                return label + "=(" + String.join(",", values) + ")";
            }
        };
    }

    /**
     * Creates a {@code label!=(value1,value2,...)}, selecting samples with the given label with a value not equal to
     * any of the values in the list.
     *
     * @param label
     *        the label, must not be {@code null}
     * @param values
     *        the values
     *
     * @return the filter
     */
    static Filter withLabelNotHavingValueFrom(String label, String... values) {
        nonNull(label, "label");
        doesNotContainNull(values, "values");
        return new Filter() {
            @Override
            public String toString() {
                return label + "!=(" + String.join(",", values) + ")";
            }
        };
    }

}
