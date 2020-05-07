package io.quarkus.panache.rest.common.deployment.methods;

import java.util.Collection;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.DataAccessImplementor;
import io.quarkus.panache.rest.common.deployment.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceAnnotator;
import io.quarkus.panache.rest.common.deployment.utils.UrlImplementor;
import io.quarkus.panache.rest.common.runtime.hal.HalCollectionWrapper;

public final class ListHalMethodImplementor implements MethodImplementor {

    private final DataAccessImplementor dataAccessImplementor;

    private final String entityClassName;

    public ListHalMethodImplementor(DataAccessImplementor dataAccessImplementor, String entityClassName) {
        this.dataAccessImplementor = dataAccessImplementor;
        this.entityClassName = entityClassName;
    }

    /**
     * Implements HAL version of {@link PanacheCrudResource#list()}.
     * Generated code looks more or less like this:
     * 
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("entities")
     *     &#64;Produces({"application/hal+json"})
     *     public HalCollectionWrapper listHal() {
     *         List entities = BookEntity.listAll();
     *         return new HalCollectionWrapper(entities, Entity.class);
     *     }
     * }
     * </pre>
     *
     * @param classCreator
     */
    @Override
    public void implement(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator.getMethodCreator("listHal", HalCollectionWrapper.class);
        ResourceAnnotator.addGet(methodCreator);
        ResourceAnnotator.addPath(methodCreator, UrlImplementor.getCollectionUrl(entityClassName));
        ResourceAnnotator.addProduces(methodCreator, ResourceAnnotator.APPLICATION_HAL_JSON);

        ResultHandle entities = dataAccessImplementor.listAll(methodCreator);
        methodCreator.returnValue(wrapEntities(methodCreator, entities, entityClassName));
        methodCreator.close();
    }

    private ResultHandle wrapEntities(BytecodeCreator creator, ResultHandle entities, String entityType) {
        return creator.newInstance(MethodDescriptor.ofConstructor(HalCollectionWrapper.class, Collection.class, Class.class),
                entities, creator.loadClass(entityType));
    }
}
