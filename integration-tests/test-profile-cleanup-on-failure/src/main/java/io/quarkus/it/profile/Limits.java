package io.quarkus.it.profile;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Limits {

    @ConfigProperty(name = "app.limit")
    int limit;

    public int limit() {
        return limit;
    }
}
