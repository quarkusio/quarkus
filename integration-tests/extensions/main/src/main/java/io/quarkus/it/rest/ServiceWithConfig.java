package io.quarkus.it.rest;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ServiceWithConfig {

    @ConfigProperty(name = "quarkus.http.host")
    String quarkusHost;

    @ConfigProperty(name = "web-message")
    String message;

    public String host() {
        return quarkusHost;
    }

    public String message() {
        return message;
    }
}
