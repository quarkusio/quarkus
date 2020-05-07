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

public final class DeleteMethodImplementor implements MethodImplementor {

    public static final String NAME = "delete";

    private final DataAccessImplementor dataAccessImplementor;

    private final String idClassName;

    private final String entityClassName;

    public DeleteMethodImplementor(DataAccessImplementor dataAccessImplementor, String idClassName, String entityClassName) {
        this.dataAccessImplementor = dataAccessImplementor;
        this.idClassName = idClassName;
        this.entityClassName = entityClassName;
    }

    /**
     * Implements {@link PanacheCrudResource#delete(Object)}.
     * Generated code looks more or less like this:
     * 
     * <pre>
     * {@code
     *     &#64;Transactional
     *     &#64;DELETE
     *     &#64;Path("entities/{id}")
     *     &#64;LinkResource(
     *         rel = "remove",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public void delete(@PathParam("id") ID id) {
     *         if (!BookEntity.deleteById(id)) {
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
        MethodCreator methodCreator = classCreator.getMethodCreator(NAME, void.class, idClassName);
        ResourceAnnotator.addTransactional(methodCreator);
        ResourceAnnotator.addDelete(methodCreator);
        ResourceAnnotator.addPath(methodCreator, UrlImplementor.getCollectionUrl(entityClassName) + "/{id}");
        ResourceAnnotator.addPathParam(methodCreator.getParameterAnnotations(0), "id");
        ResourceAnnotator.addLinks(methodCreator, entityClassName, "remove");

        ResultHandle result = dataAccessImplementor.deleteById(methodCreator, methodCreator.getMethodParam(0));
        BranchResult entityWasDeleted = methodCreator.ifNonZero(result);

        entityWasDeleted.trueBranch().returnValue(null);
        entityWasDeleted.falseBranch().throwException(ResponseImplementor.notFoundException(entityWasDeleted.falseBranch()));
        methodCreator.close();
    }
}
