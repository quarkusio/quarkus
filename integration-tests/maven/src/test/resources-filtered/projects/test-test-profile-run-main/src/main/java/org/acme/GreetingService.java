package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {
    public String greet(String greeting) {
        return "Hello " + greeting;
    }
}
