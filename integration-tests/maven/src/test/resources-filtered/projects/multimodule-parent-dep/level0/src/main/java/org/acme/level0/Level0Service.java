package org.acme.level0;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Level0Service {

    @ConfigProperty(name = "greeting")
    String greeting;

    public String getGreeting() {
        return greeting;
    }
}
