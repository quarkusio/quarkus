package io.quarkus.rest.data.panache.deployment.methods;

import io.quarkus.deployment.Capabilities;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;

/**
 * Loop over all the user-defined methods in the interface. These methods will be overridden in the generated resource
 * if and only if it uses annotations.
 */
public final class UserMethodsWithAnnotationsImplementor implements MethodImplementor {

    private final Capabilities capabilities;

    public UserMethodsWithAnnotationsImplementor(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void implement(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        if (resourceMetadata.getResourceInterface() != null) {
            for (var methodInfo : resourceMetadata.getResourceInterface().methods()) {
                // propagate only the default methods with annotations (needed for `@Transactional` annotations).
                if (methodInfo.isDefault() && !methodInfo.annotations().isEmpty()) {
                    OverrideUserMethodImplementor implementor = new OverrideUserMethodImplementor(methodInfo,
                            capabilities);
                    implementor.implement(classCreator, resourceMetadata, resourceProperties, resourceField);
                }
            }
        }
    }
}
