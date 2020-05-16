package io.quarkus.rest.data.panache.deployment.methods.hal;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.methods.ListMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodMetadata;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.runtime.hal.HalCollectionWrapper;

public final class ListHalMethodImplementor extends HalMethodImplementor {

    private static final String NAME = "listHal";

    /**
     * Implements HAL version of {@link RestDataResource#list()}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("")
     *     &#64;Produces({"application/hal+json"})
     *     public HalCollectionWrapper listHal() {
     *         List entities = BookEntity.listAll();
     *         return new HalCollectionWrapper(entities, Entity.class, "entities");
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodCreator methodCreator = classCreator.getMethodCreator(NAME, HalCollectionWrapper.class);
        addGetAnnotation(methodCreator);
        addPathAnnotation(methodCreator,
                propertiesAccessor.getPath(resourceInfo.getClassInfo(), getStandardMethodMetadata(resourceInfo)));
        addProducesAnnotation(methodCreator, MethodImplementor.APPLICATION_HAL_JSON);

        ResultHandle entities = resourceInfo.getDataAccessImplementor().listAll(methodCreator);
        methodCreator.returnValue(wrapEntities(methodCreator, entities, resourceInfo));
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getStandardMethodMetadata(RestDataResourceInfo resourceInfo) {
        return new MethodMetadata(ListMethodImplementor.NAME);
    }
}
