package io.quarkus.undertow.test;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestGreeter {

    public String message() {
        return "test servlet";
    }
}
