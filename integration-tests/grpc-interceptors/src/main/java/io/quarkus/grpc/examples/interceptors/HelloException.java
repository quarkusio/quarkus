package io.quarkus.grpc.examples.interceptors;

public class HelloException extends RuntimeException {
    private final String name;

    public HelloException(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
