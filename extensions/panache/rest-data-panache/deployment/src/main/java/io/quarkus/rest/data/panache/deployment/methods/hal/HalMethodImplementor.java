package io.quarkus.rest.data.panache.deployment.methods.hal;

import java.util.Collection;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.methods.StandardMethodImplementor;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.runtime.hal.HalCollectionWrapper;
import io.quarkus.rest.data.panache.runtime.hal.HalEntityWrapper;

/**
 * HAL JAX-RS method implementor.
 */
abstract class HalMethodImplementor extends StandardMethodImplementor {

    HalMethodImplementor(boolean isResteasyClassic) {
        super(isResteasyClassic);
    }

    /**
     * Implement a method if it is exposed and hal is enabled.
     */
    @Override
    public void implement(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        if (resourceProperties.isHal() && resourceProperties.isExposed(getResourceMethodName())) {
            implementInternal(classCreator, resourceMetadata, resourceProperties, resourceField);
        }
    }

    protected ResultHandle wrapHalEntity(BytecodeCreator creator, ResultHandle entity) {
        return creator.newInstance(MethodDescriptor.ofConstructor(HalEntityWrapper.class, Object.class), entity);
    }

    protected ResultHandle wrapHalEntities(BytecodeCreator creator, ResultHandle entities, String entityType,
            String collectionName) {
        return creator.newInstance(
                MethodDescriptor.ofConstructor(HalCollectionWrapper.class, Collection.class, Class.class, String.class),
                entities, creator.loadClass(entityType),
                creator.load(collectionName));
    }
}
