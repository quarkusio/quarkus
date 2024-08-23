package io.quarkus.it.rest;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ServiceWithConfig {

    @ConfigProperty(name = "quarkus.http.host")
    String quarkusHost;

    @ConfigProperty(name = "web-message")
    String message;

    @ConfigProperty(name = "names")
    String[] names;

    public String host() {
        return quarkusHost;
    }

    public String message() {
        return message;
    }

    public String[] names() {
        return names;
    }
}
