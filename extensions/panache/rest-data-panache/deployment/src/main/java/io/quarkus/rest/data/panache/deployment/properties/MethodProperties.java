package io.quarkus.rest.data.panache.deployment.properties;

public class MethodProperties {

    private final boolean exposed;

    private final String path;

    private final String[] rolesAllowed;

    public MethodProperties(boolean exposed, String path, String[] rolesAllowed) {
        this.exposed = exposed;
        this.path = path;
        this.rolesAllowed = rolesAllowed;
    }

    public boolean isExposed() {
        return exposed;
    }

    public String getPath() {
        return path;
    }

    public String[] getRolesAllowed() {
        return rolesAllowed;
    }
}
