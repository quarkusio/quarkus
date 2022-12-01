package io.quarkus.resteasy.reactive.server.test.response;

@SuppressWarnings("serial")
public class UnknownCheeseException2 extends RuntimeException {

    public String name;

    public UnknownCheeseException2(String name) {
        this.name = name;
    }
}
