package org.acme.greeter;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Greeter {
    public String getGreeting() {
        return "Hello";
    }
}
