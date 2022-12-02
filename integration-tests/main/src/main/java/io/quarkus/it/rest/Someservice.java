package io.quarkus.it.rest;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Someservice {

    public String name() {
        return "some";
    }
}
