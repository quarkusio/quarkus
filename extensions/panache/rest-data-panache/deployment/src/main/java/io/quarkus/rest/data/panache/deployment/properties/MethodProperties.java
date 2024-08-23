package io.quarkus.rest.data.panache.deployment.properties;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;

public class MethodProperties {

    private final boolean exposed;

    private final String path;

    private final String[] rolesAllowed;

    private final Collection<AnnotationInstance> methodAnnotations;

    public MethodProperties(boolean exposed, String path, String[] rolesAllowed,
            Collection<AnnotationInstance> methodAnnotations) {
        this.exposed = exposed;
        this.path = path;
        this.rolesAllowed = rolesAllowed;
        this.methodAnnotations = methodAnnotations;
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

    public Collection<AnnotationInstance> getMethodAnnotations() {
        return methodAnnotations;
    }
}
