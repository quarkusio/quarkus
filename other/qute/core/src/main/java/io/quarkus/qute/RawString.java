package io.quarkus.qute;

/**
 * Raw string is never escaped.
 */
public final class RawString {

    private final String value;

    public RawString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
