package io.quarkus.runtime.logging;

/**
 * The type of formatter to use.
 */
public enum FormatType {
    /**
     * Format in text for person-readability.
     */
    TEXT,
    /**
     * Format in JSON for machine-readability.
     */
    JSON,
    ;

    public static FormatType of(String str) {
        if (str.equalsIgnoreCase("text")) {
            return TEXT;
        } else if (str.equalsIgnoreCase("json")) {
            return JSON;
        } else {
            throw new IllegalArgumentException("Unrecognized value: " + str);
        }
    }
}
