package org.acme.invokedyn;

public class LambdaTarget {
    private final String value;

    public LambdaTarget(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
