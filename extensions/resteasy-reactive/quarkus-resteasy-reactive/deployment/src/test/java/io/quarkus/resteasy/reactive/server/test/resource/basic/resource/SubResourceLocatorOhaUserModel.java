package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

public class SubResourceLocatorOhaUserModel {
    private String username;

    public SubResourceLocatorOhaUserModel(final String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return username;
    }
}
