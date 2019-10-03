package io.quarkus.amazon.lambda.http.test.model;

/**
 * Request/response model
 */
public class SingleValueModel {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}