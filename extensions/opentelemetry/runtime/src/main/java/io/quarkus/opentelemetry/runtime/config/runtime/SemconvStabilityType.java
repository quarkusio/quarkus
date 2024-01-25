package io.quarkus.opentelemetry.runtime.config.runtime;

public enum SemconvStabilityType {
    /**
     * Emit the new stable convention names
     */
    HTTP("http"),
    /**
     * Emit the old unstable plus the new stable convention names for transition purposes
     */
    HTTP_DUP("http/dup"),
    /**
     * Emit the old unstable convention names.
     * <p>
     * This is a non standard property name, only used in Quarkus.
     */
    HTTP_OLD("http/old");

    private final String value;

    SemconvStabilityType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SemconvStabilityType fromValue(String value) {
        for (SemconvStabilityType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant with value: " + value);
    }

    static class Constants {
        public static final String HTTP = "http";
        public static final String HTTP_DUP = "http/dup";
        public static final String HTTP_OLD = "http/old";
    }
}
