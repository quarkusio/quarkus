package io.quarkus.quickcli;

/**
 * Metadata for a positional parameter, built at compile time.
 */
public final class ParameterSpec {

    private final int index;
    private final String description;
    private final Class<?> type;
    private final String defaultValue;
    private final String arity;
    private final boolean hidden;
    private final String paramLabel;
    private final String fieldName;
    private final boolean isMultiValue;

    public ParameterSpec(int index, String description, Class<?> type,
                         String defaultValue, String arity, boolean hidden,
                         String paramLabel, String fieldName, boolean isMultiValue) {
        this.index = index;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue;
        this.arity = arity;
        this.hidden = hidden;
        this.paramLabel = paramLabel;
        this.fieldName = fieldName;
        this.isMultiValue = isMultiValue;
    }

    public int index() {
        return index;
    }

    public String description() {
        return description;
    }

    public Class<?> type() {
        return type;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String arity() {
        return arity;
    }

    public boolean hidden() {
        return hidden;
    }

    public String paramLabel() {
        return paramLabel;
    }

    public String fieldName() {
        return fieldName;
    }

    public boolean isMultiValue() {
        return isMultiValue;
    }
}
