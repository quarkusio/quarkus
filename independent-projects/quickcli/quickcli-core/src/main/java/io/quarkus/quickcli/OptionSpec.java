package io.quarkus.quickcli;

/**
 * Metadata for a command-line option, built at compile time.
 */
public final class OptionSpec {

    private final String[] names;
    private final String description;
    private final Class<?> type;
    private final boolean required;
    private final String defaultValue;
    private final String arity;
    private final boolean hidden;
    private final String paramLabel;
    private final String fieldName;
    private final boolean isBoolean;
    private final boolean versionHelp;
    private final boolean usageHelp;
    private final boolean negatable;
    private final int order;
    private Object value; // stores the matched value for matchedOption() access

    public OptionSpec(String[] names, String description, Class<?> type,
                      boolean required, String defaultValue, String arity,
                      boolean hidden, String paramLabel, String fieldName) {
        this(names, description, type, required, defaultValue, arity,
                hidden, paramLabel, fieldName, false, -1);
    }

    public OptionSpec(String[] names, String description, Class<?> type,
                      boolean required, String defaultValue, String arity,
                      boolean hidden, String paramLabel, String fieldName,
                      boolean versionHelp, int order) {
        this(names, description, type, required, defaultValue, arity,
                hidden, paramLabel, fieldName, versionHelp, false, false, order);
    }

    public OptionSpec(String[] names, String description, Class<?> type,
                      boolean required, String defaultValue, String arity,
                      boolean hidden, String paramLabel, String fieldName,
                      boolean versionHelp, boolean usageHelp, boolean negatable,
                      int order) {
        this.names = names;
        this.description = description;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.arity = arity;
        this.hidden = hidden;
        this.paramLabel = paramLabel;
        this.fieldName = fieldName;
        this.isBoolean = TypeConverter.isBooleanType(type);
        this.versionHelp = versionHelp;
        this.usageHelp = usageHelp;
        this.negatable = negatable;
        this.order = order;
    }

    public String[] names() {
        return names;
    }

    /** Returns the longest name (typically the --long-form). */
    public String longestName() {
        String longest = names[0];
        for (String name : names) {
            if (name.length() > longest.length()) {
                longest = name;
            }
        }
        return longest;
    }

    public String description() {
        return description;
    }

    public Class<?> type() {
        return type;
    }

    public boolean required() {
        return required;
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

    public boolean isBoolean() {
        return isBoolean;
    }

    public boolean isVersionHelp() {
        return versionHelp;
    }

    public boolean isUsageHelp() {
        return usageHelp;
    }

    public boolean isNegatable() {
        return negatable;
    }

    public int order() {
        return order;
    }

    /** Get the matched value (set during parsing). */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    /** Set the matched value. */
    public void setValue(Object value) {
        this.value = value;
    }

    /** Check if the given argument matches any of this option's names. */
    public boolean matches(String arg) {
        for (String name : names) {
            if (name.equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
