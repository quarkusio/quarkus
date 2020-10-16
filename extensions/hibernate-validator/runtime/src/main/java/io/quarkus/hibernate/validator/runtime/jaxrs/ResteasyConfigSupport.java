package io.quarkus.hibernate.validator.runtime.jaxrs;

public class ResteasyConfigSupport {

    private boolean jsonDefault;

    public ResteasyConfigSupport() {
    }

    public ResteasyConfigSupport(boolean jsonDefault) {
        this.jsonDefault = jsonDefault;
    }

    public boolean isJsonDefault() {
        return jsonDefault;
    }
}
