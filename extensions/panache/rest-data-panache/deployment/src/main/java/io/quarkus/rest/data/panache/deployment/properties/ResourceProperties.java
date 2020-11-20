package io.quarkus.rest.data.panache.deployment.properties;

import java.util.Map;

public class ResourceProperties {

    private final boolean hal;

    private final String path;

    private final boolean paged;

    private final String halCollectionName;

    private final Map<String, MethodProperties> methodProperties;

    public ResourceProperties(boolean hal, String path, boolean paged, String halCollectionName,
            Map<String, MethodProperties> methodProperties) {
        this.hal = hal;
        this.path = path;
        this.paged = paged;
        this.halCollectionName = halCollectionName;
        this.methodProperties = methodProperties;
    }

    public boolean isHal() {
        return hal;
    }

    public String getPath() {
        return path;
    }

    public String getMethodPath(String methodName) {
        if (methodProperties.containsKey(methodName)) {
            return methodProperties.get(methodName).getPath();
        }
        return "";
    }

    public boolean isPaged() {
        return paged;
    }

    public boolean isExposed(String methodName) {
        if (methodProperties.containsKey(methodName)) {
            return methodProperties.get(methodName).isExposed();
        }
        return true;
    }

    public String getHalCollectionName() {
        return halCollectionName;
    }
}
