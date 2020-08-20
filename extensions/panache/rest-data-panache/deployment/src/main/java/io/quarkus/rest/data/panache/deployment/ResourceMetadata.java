package io.quarkus.rest.data.panache.deployment;

public class ResourceMetadata {

    /**
     * Generated class that implements RestDataResource.
     */
    private final String resourceClass;

    /**
     * Application interface that extends RestDataResource interface.
     */
    private final String resourceInterface;

    /**
     * Entity class that is used by the resource.
     */
    private final String entityType;

    /**
     * ID class/interface that is used by the resource.
     */
    private final String idType;

    public ResourceMetadata(String resourceClass, String resourceInterface, String entityType, String idType) {
        this.resourceClass = resourceClass;
        this.resourceInterface = resourceInterface;
        this.entityType = entityType;
        this.idType = idType;
    }

    public String getResourceClass() {
        return resourceClass;
    }

    public String getResourceInterface() {
        return resourceInterface;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getIdType() {
        return idType;
    }
}
