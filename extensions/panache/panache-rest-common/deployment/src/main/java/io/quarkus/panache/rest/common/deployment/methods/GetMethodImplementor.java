package io.quarkus.panache.rest.common.deployment.methods;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.DataAccessImplementor;
import io.quarkus.panache.rest.common.deployment.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceAnnotator;
import io.quarkus.panache.rest.common.deployment.utils.ResponseImplementor;
import io.quarkus.panache.rest.common.deployment.utils.UrlImplementor;

public final class GetMethodImplementor implements MethodImplementor {

    public static final String NAME = "get";

    private final DataAccessImplementor dataAccessImplementor;

    private final String idClassName;

    private final String entityClassName;

    public GetMethodImplementor(DataAccessImplementor dataAccessImplementor, String idClassName, String entityClassName) {
        this.dataAccessImplementor = dataAccessImplementor;
        this.idClassName = idClassName;
        this.entityClassName = entityClassName;
    }

    /**
     * Implements {@link PanacheCrudResource#get(Object)}.
     * Generated code looks more or less like this:
     * 
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Produces({"application/json"})
     *     &#64;Path("entities/{id}")
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
     *
     * @param classCreator
     */
    @Override
    public void implement(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator.getMethodCreator(NAME, entityClassName, idClassName);
        ResourceAnnotator.addGet(methodCreator);
        ResourceAnnotator.addProduces(methodCreator, ResourceAnnotator.APPLICATION_JSON);
        ResourceAnnotator.addPath(methodCreator, UrlImplementor.getCollectionUrl(entityClassName) + "/{id}");
        ResourceAnnotator.addPathParam(methodCreator.getParameterAnnotations(0), "id");
        ResourceAnnotator.addLinks(methodCreator, entityClassName, "self");

        ResultHandle entity = dataAccessImplementor.findById(methodCreator, methodCreator.getMethodParam(0));
        BranchResult entityNotFound = methodCreator.ifNull(entity);

        entityNotFound.trueBranch().throwException(ResponseImplementor.notFoundException(entityNotFound.trueBranch()));
        entityNotFound.falseBranch().returnValue(entity);
        methodCreator.close();
    }
}
