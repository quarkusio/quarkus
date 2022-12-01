package io.quarkus.undertow.test;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestGreeter {

    public String message() {
        return "test servlet";
    }
}
