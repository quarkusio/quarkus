package io.quarkus.panache.rest.common.deployment.methods;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.DataAccessImplementor;
import io.quarkus.panache.rest.common.deployment.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceAnnotator;
import io.quarkus.panache.rest.common.deployment.utils.ResponseImplementor;
import io.quarkus.panache.rest.common.deployment.utils.UrlImplementor;
import io.quarkus.panache.rest.common.runtime.hal.HalEntityWrapper;

public final class GetHalMethodImplementor implements MethodImplementor {

    private final DataAccessImplementor dataAccessImplementor;

    private final String idClassName;

    private final String entityClassName;

    public GetHalMethodImplementor(DataAccessImplementor dataAccessImplementor, String idClassName, String entityClassName) {
        this.dataAccessImplementor = dataAccessImplementor;
        this.idClassName = idClassName;
        this.entityClassName = entityClassName;
    }

    /**
     * Implements HAL version of {@link PanacheCrudResource#get(Object)}.
     * Generated code looks more or less like this:
     * 
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Produces({"application/hal+json"})
     *     &#64;Path("entities/{id}")
     *     public HalEntityWrapper getHal(@PathParam("id") ID id) {
     *         PanacheEntityBase entity = BookEntity.findById(id);
     *         if (entity != null) {
     *             return new HalEntityWrapper(entity);
     *         } else {
     *             throw new WebApplicationException(404);
     *         }
     *     }
     * }
     * </pre>
     *
     * @param classCreator
     */
    @Override
    public void implement(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator.getMethodCreator("getHal", HalEntityWrapper.class, idClassName);
        ResourceAnnotator.addGet(methodCreator);
        ResourceAnnotator.addProduces(methodCreator, ResourceAnnotator.APPLICATION_HAL_JSON);
        ResourceAnnotator.addPath(methodCreator, UrlImplementor.getCollectionUrl(entityClassName) + "/{id}");
        ResourceAnnotator.addPathParam(methodCreator.getParameterAnnotations(0), "id");

        ResultHandle entity = dataAccessImplementor.findById(methodCreator, methodCreator.getMethodParam(0));
        BranchResult entityNotFound = methodCreator.ifNull(entity);

        entityNotFound.trueBranch().throwException(ResponseImplementor.notFoundException(entityNotFound.trueBranch()));
        entityNotFound.falseBranch().returnValue(wrapHalEntity(entityNotFound.falseBranch(), entity));
        methodCreator.close();
    }

    private ResultHandle wrapHalEntity(BytecodeCreator creator, ResultHandle entity) {
        return creator.newInstance(MethodDescriptor.ofConstructor(HalEntityWrapper.class, Object.class), entity);
    }
}
