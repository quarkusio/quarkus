package io.quarkus.resteasy.reactive.server.test.customproviders;

public class UniException extends RuntimeException {

    private final String input;

    public UniException(String input) {
        super("Failed with input: " + input);
        this.input = input;
    }

    public String getInput() {
        return input;
    }
}
