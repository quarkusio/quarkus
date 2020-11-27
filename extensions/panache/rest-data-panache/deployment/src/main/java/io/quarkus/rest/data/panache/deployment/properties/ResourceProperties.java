package io.quarkus.rest.data.panache.deployment.properties;

import java.util.Map;

public class ResourceProperties {

    private final boolean exposed;

    private final String path;

    private final boolean paged;

    private final boolean hal;

    private final String halCollectionName;

    private final Map<String, MethodProperties> methodProperties;

    public ResourceProperties(boolean exposed, String path, boolean paged, boolean hal, String halCollectionName,
            Map<String, MethodProperties> methodProperties) {
        this.exposed = exposed;
        this.path = path;
        this.paged = paged;
        this.hal = hal;
        this.halCollectionName = halCollectionName;
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
}
