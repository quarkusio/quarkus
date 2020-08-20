package io.quarkus.rest.data.panache.deployment.properties;

public class MethodProperties {

    private final boolean exposed;

    private final String path;

    public MethodProperties(boolean exposed, String path) {
        this.exposed = exposed;
        this.path = path;
    }

    public boolean isExposed() {
        return exposed;
    }

    public String getPath() {
        return path;
    }
}
