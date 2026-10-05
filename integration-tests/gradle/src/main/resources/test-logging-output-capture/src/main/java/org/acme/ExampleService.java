package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExampleService {

    public String greet() {
        return "hello";
    }
}
