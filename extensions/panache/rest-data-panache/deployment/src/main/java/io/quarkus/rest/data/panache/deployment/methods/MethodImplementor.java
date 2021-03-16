package io.quarkus.rest.data.panache.deployment.methods;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;

/**
 * JAX-RS method implementor.
 */
public interface MethodImplementor {

    String APPLICATION_JSON = "application/json";

    String APPLICATION_HAL_JSON = "application/hal+json";

    /**
     * Implement a specific JAX-RS method using the provided resource metadata.
     */
    void implement(ClassCreator classCreator, ResourceMetadata resourceMetadata, ResourceProperties resourceProperties,
            FieldDescriptor resourceField);
}
