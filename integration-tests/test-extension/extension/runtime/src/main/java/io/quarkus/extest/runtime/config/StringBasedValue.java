package io.quarkus.extest.runtime.config;

public class StringBasedValue {
    private String value;

    public StringBasedValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
