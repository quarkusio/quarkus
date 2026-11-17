package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

import org.acme.common.Greeting;

@ApplicationScoped
public class App {

    public String greet() {
        return Greeting.hello();
    }
}
