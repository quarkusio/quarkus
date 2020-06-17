package io.quarkus.rest.data.panache.deployment.methods;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;

public final class GetMethodImplementor extends StandardMethodImplementor {

    public static final String NAME = "get";

    private static final String REL = "self";

    /**
     * Implements {@link RestDataResource#get(Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Produces({"application/json"})
     *     &#64;Path("{id}")
     *     &#64;LinkResource(
     *         rel = "self",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Entity get(@PathParam("id") ID id) {
     *         Entity entity = Entity.findById(id);
     *         if (entity != null) {
     *             return entity;
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
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        MethodCreator methodCreator = classCreator.getMethodCreator(methodMetadata.getName(),
                resourceInfo.getEntityInfo().getType(), methodMetadata.getParameterTypes());
        addGetAnnotation(methodCreator);
        addProducesAnnotation(methodCreator, APPLICATION_JSON);
        addPathAnnotation(methodCreator, propertiesAccessor.getPath(resourceInfo.getType(), methodMetadata, "{id}"));
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addLinksAnnotation(methodCreator, resourceInfo.getEntityInfo().getType(), REL);

        ResultHandle entity = resourceInfo.getDataAccessImplementor().findById(methodCreator, methodCreator.getMethodParam(0));
        BranchResult entityNotFound = methodCreator.ifNull(entity);

        entityNotFound.trueBranch().throwException(ResponseImplementor.notFoundException(entityNotFound.trueBranch()));
        entityNotFound.falseBranch().returnValue(entity);
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getMethodMetadata(RestDataResourceInfo resourceInfo) {
        return new MethodMetadata(NAME, resourceInfo.getEntityInfo().getIdType());
    }
}
