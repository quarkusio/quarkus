package io.quarkus.rest.data.panache.deployment.methods.hal;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.Collection;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hal.HalCollectionWrapper;
import io.quarkus.hal.HalService;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.methods.StandardMethodImplementor;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.resteasy.links.runtime.hal.ResteasyHalService;
import io.quarkus.resteasy.reactive.links.runtime.hal.ResteasyReactiveHalService;

/**
 * HAL JAX-RS method implementor.
 */
abstract class HalMethodImplementor extends StandardMethodImplementor {

    HalMethodImplementor(boolean isResteasyClassic, boolean isReactivePanache) {
        super(isResteasyClassic, isReactivePanache);
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

    protected ResultHandle wrapHalEntities(BytecodeCreator creator, ResultHandle entities, String entityType,
            String collectionName) {
        ResultHandle arcContainer = creator.invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = creator.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class),
                arcContainer,
                creator.loadClassFromTCCL(isResteasyClassic() ? ResteasyHalService.class : ResteasyReactiveHalService.class),
                creator.newArray(Annotation.class, 0));
        ResultHandle halService = creator.invokeInterfaceMethod(
                ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);

        return creator.invokeVirtualMethod(MethodDescriptor.ofMethod(HalService.class, "toHalCollectionWrapper",
                HalCollectionWrapper.class, Collection.class, String.class, Class.class),
                halService, entities, creator.load(collectionName), creator.loadClassFromTCCL(entityType));
    }
}
