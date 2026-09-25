package org.acme.gradledemo.extension;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DemoGreeting {

    public String message() {
        return "Hello from the included extension";
    }
}
