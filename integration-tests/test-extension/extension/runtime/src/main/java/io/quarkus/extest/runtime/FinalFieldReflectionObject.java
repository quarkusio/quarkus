package io.quarkus.extest.runtime;

public class FinalFieldReflectionObject {
    private final String value;

    public FinalFieldReflectionObject() {
        this.value = null;
    }

    public FinalFieldReflectionObject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
