package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class Greeting {

    public String message() {
        return "hello 0";
    }
}
