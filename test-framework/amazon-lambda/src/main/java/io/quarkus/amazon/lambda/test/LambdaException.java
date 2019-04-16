package io.quarkus.amazon.lambda.test;

public class LambdaException extends RuntimeException {
    final String type;

    public LambdaException(String type, String message) {
        super(message);
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
