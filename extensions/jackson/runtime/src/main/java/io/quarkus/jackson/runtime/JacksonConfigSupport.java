package io.quarkus.jackson.runtime;

public class JacksonConfigSupport {

    private boolean failOnUnknownProperties;

    public JacksonConfigSupport(boolean failOnUnknownProperties) {
        this.failOnUnknownProperties = failOnUnknownProperties;
    }

    public boolean isFailOnUnknownProperties() {
        return failOnUnknownProperties;
    }
}
