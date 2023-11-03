package io.quarkus.it.rest;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Someservice {

    public String name() {
        return "some";
    }
}
