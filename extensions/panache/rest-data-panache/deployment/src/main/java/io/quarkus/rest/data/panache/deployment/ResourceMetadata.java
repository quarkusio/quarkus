package io.quarkus.rest.data.panache.deployment;

import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Type;

public class ResourceMetadata {

    /**
     * Generated class that implements RestDataResource.
     */
    private final String resourceClass;

    /**
     * Name that is used to generate the new resource.
     */
    private final String resourceName;

    /**
     * Application interface that extends RestDataResource interface.
     */
    private final ClassInfo resourceInterface;

    /**
     * Entity class that is used by the resource.
     */
    private final String entityType;

    /**
     * ID class/interface that is used by the resource.
     */
    private final String idType;

    /**
     * Map containing the field names by field types.
     */
    private final Map<String, Type> fields;

    /**
     * Map of field names to required roles for @SecureField annotated fields.
     * Format: fieldName -> "role1,role2,role3"
     */
    private final Map<String, String> secureFields;

    public ResourceMetadata(String resourceClass, ClassInfo resourceInterface, String entityType, String idType,
            Map<String, Type> fields) {
        this(resourceClass, resourceInterface.name().toString(), resourceInterface, entityType, idType, fields,
                java.util.Collections.emptyMap());
    }

    public ResourceMetadata(String resourceClass, ClassInfo resourceInterface, String entityType, String idType,
            Map<String, Type> fields, Map<String, String> secureFields) {
        this(resourceClass, resourceInterface.name().toString(), resourceInterface, entityType, idType, fields, secureFields);
    }

    public ResourceMetadata(String resourceClass, String resourceName, ClassInfo resourceInterface, String entityType,
            String idType, Map<String, Type> fields) {
        this(resourceClass, resourceName, resourceInterface, entityType, idType, fields, java.util.Collections.emptyMap());
    }

    public ResourceMetadata(String resourceClass, String resourceName, ClassInfo resourceInterface, String entityType,
            String idType, Map<String, Type> fields, Map<String, String> secureFields) {
        this.resourceClass = resourceClass;
        this.resourceName = resourceName;
        this.resourceInterface = resourceInterface;
        this.entityType = entityType;
        this.idType = idType;
        this.fields = fields;
        this.secureFields = secureFields;
    }

    public String getResourceClass() {
        return resourceClass;
    }

    public String getResourceName() {
        return resourceName;
    }

    public ClassInfo getResourceInterface() {
        return resourceInterface;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getIdType() {
        return idType;
    }

    public Map<String, Type> getFields() {
        return fields;
    }

    public Map<String, String> getSecureFields() {
        return secureFields;
    }
}
