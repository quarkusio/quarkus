package io.quarkus.panache.rest.common.deployment.methods.hal;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceInfo;
import io.quarkus.panache.rest.common.deployment.methods.ListMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.MethodMetadata;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;
import io.quarkus.panache.rest.common.runtime.hal.HalCollectionWrapper;

public final class ListHalMethodImplementor extends HalMethodImplementor {

    private static final String NAME = "listHal";

    /**
     * Implements HAL version of {@link PanacheCrudResource#list()}.
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
    protected void implementInternal(ClassCreator classCreator, IndexView index, OperationPropertiesAccessor propertiesAccessor,
            PanacheCrudResourceInfo resourceInfo) {
        MethodCreator methodCreator = classCreator.getMethodCreator(NAME, HalCollectionWrapper.class);
        addGetAnnotation(methodCreator);
        addPathAnnotation(methodCreator,
                propertiesAccessor.getPath(resourceInfo.getResourceClassInfo(), getStandardMethodMetadata(resourceInfo)));
        addProducesAnnotation(methodCreator, MethodImplementor.APPLICATION_HAL_JSON);

        ResultHandle entities = resourceInfo.getDataAccessImplementor().listAll(methodCreator);
        methodCreator.returnValue(wrapEntities(methodCreator, entities, resourceInfo));
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getStandardMethodMetadata(PanacheCrudResourceInfo resourceInfo) {
        return new MethodMetadata(ListMethodImplementor.NAME);
    }
}
