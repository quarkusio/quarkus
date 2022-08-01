package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

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
