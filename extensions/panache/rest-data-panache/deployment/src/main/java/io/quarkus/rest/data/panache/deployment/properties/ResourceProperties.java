package io.quarkus.rest.data.panache.deployment.properties;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

public class ResourceProperties {

    private final boolean exposed;

    private final String path;

    private final boolean paged;

    private final boolean hal;

    private final String halCollectionName;

    private final String[] rolesAllowed;

    private final boolean isAuthenticated;

    private final Collection<AnnotationInstance> classAnnotations;

    private final Map<String, MethodProperties> methodProperties;

    public ResourceProperties(boolean exposed, String path, boolean paged, boolean hal, String halCollectionName,
            String[] rolesAllowed, boolean isAuthenticated, Collection<AnnotationInstance> classAnnotations,
            Map<String, MethodProperties> methodProperties) {
        this.exposed = exposed;
        this.path = path;
        this.paged = paged;
        this.hal = hal;
        this.halCollectionName = halCollectionName;
        this.rolesAllowed = rolesAllowed;
        this.isAuthenticated = isAuthenticated;
        this.classAnnotations = classAnnotations;
        this.methodProperties = methodProperties;
    }

    public boolean isExposed() {
        if (exposed) {
            return true;
        }
        // If at least one method is explicitly exposed, resource should also be exposed
        for (MethodProperties properties : this.methodProperties.values()) {
            if (properties.isExposed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isExposed(String methodName) {
        if (methodProperties.containsKey(methodName)) {
            return methodProperties.get(methodName).isExposed();
        }
        return exposed;
    }

    public String getPath() {
        return path;
    }

    public String getPath(String methodName) {
        if (methodProperties.containsKey(methodName)) {
            return methodProperties.get(methodName).getPath();
        }
        return "";
    }

    public boolean isPaged() {
        return paged;
    }

    public boolean isHal() {
        return hal;
    }

    public String getHalCollectionName() {
        return halCollectionName;
    }

    public String[] getRolesAllowed(String methodName) {
        if (methodProperties.containsKey(methodName)) {
            return methodProperties.get(methodName).getRolesAllowed();
        }

        return rolesAllowed;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public Collection<AnnotationInstance> getClassAnnotations() {
        return classAnnotations;
    }

    public Collection<AnnotationInstance> getMethodAnnotations(String methodName) {
        if (methodProperties.containsKey(methodName)) {
            return methodProperties.get(methodName).getMethodAnnotations();
        }

        return Collections.emptyList();
    }
}
