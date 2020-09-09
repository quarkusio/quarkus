package io.quarkus.rest.data.panache.deployment.methods.hal;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.methods.GetMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodMetadata;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;
import io.quarkus.rest.data.panache.runtime.hal.HalEntityWrapper;

public final class GetHalMethodImplementor extends HalMethodImplementor {

    private static final String NAME = "getHal";

    /**
     * Implements HAL version of {@link RestDataResource#get(Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Produces({"application/hal+json"})
     *     &#64;Path("{id}")
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
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodCreator methodCreator = classCreator.getMethodCreator(NAME, HalEntityWrapper.class.getName(),
                resourceInfo.getEntityInfo().getIdType());
        addGetAnnotation(methodCreator);
        addProducesAnnotation(methodCreator, MethodImplementor.APPLICATION_HAL_JSON);
        addPathAnnotation(methodCreator, propertiesAccessor
                .getPath(resourceInfo.getType(), getMethodMetadata(resourceInfo), "{id}"));
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");

        ResultHandle entity = resourceInfo.getDataAccessImplementor().findById(methodCreator, methodCreator.getMethodParam(0));
        BranchResult entityNotFound = methodCreator.ifNull(entity);

        entityNotFound.trueBranch().throwException(ResponseImplementor.notFoundException(entityNotFound.trueBranch()));
        entityNotFound.falseBranch().returnValue(wrapHalEntity(entityNotFound.falseBranch(), entity));
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getMethodMetadata(RestDataResourceInfo resourceInfo) {
        return new MethodMetadata(GetMethodImplementor.NAME, resourceInfo.getEntityInfo().getIdType());
    }
}
