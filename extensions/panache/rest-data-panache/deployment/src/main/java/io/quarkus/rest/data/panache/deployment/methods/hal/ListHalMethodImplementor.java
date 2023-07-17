package io.quarkus.rest.data.panache.deployment.methods.hal;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.Collection;

import jakarta.ws.rs.core.Link;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.deployment.Capabilities;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hal.HalCollectionWrapper;
import io.quarkus.hal.HalService;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.methods.ListMethodImplementor;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.resteasy.links.runtime.hal.ResteasyHalService;
import io.quarkus.resteasy.reactive.links.runtime.hal.ResteasyReactiveHalService;

public final class ListHalMethodImplementor extends ListMethodImplementor {

    private static final String METHOD_NAME = "listHal";

    public ListHalMethodImplementor(Capabilities capabilities) {
        super(capabilities);
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

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    protected void addProducesJsonAnnotation(AnnotatedElement element, ResourceProperties properties) {
        super.addProducesAnnotation(element, APPLICATION_HAL_JSON);
    }

    @Override
    protected void returnValueWithLinks(BytecodeCreator creator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, ResultHandle value, ResultHandle links) {
        ResultHandle wrapper = wrapHalEntities(creator, value, resourceMetadata.getEntityType(),
                resourceProperties.getHalCollectionName());

        creator.invokeVirtualMethod(
                ofMethod(HalCollectionWrapper.class, "addLinks", void.class, Link[].class), wrapper, links);
        creator.returnValue(responseImplementor.ok(creator, wrapper, links));
    }

    @Override
    protected void returnValue(BytecodeCreator creator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, ResultHandle value) {
        ResultHandle wrapper = wrapHalEntities(creator, value, resourceMetadata.getEntityType(),
                resourceProperties.getHalCollectionName());
        creator.returnValue(responseImplementor.ok(creator, wrapper));
    }

    private ResultHandle wrapHalEntities(BytecodeCreator creator, ResultHandle entities, String entityType,
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
