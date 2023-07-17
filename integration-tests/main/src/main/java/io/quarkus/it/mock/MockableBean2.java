package io.quarkus.it.mock;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MockableBean2 {

    public String greet(String name) {
        return "Hello " + name;
    }
}
