package io.quarkus.panache.rest.common.deployment.methods;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceInfo;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;
import io.quarkus.panache.rest.common.deployment.utils.ResponseImplementor;

public final class DeleteMethodImplementor extends StandardMethodImplementor {

    private static final String NAME = "delete";

    private static final String REL = "remove";

    /**
     * Implements {@link PanacheCrudResource#delete(Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;Transactional
     *     &#64;DELETE
     *     &#64;Path("{id}")
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
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, IndexView index, OperationPropertiesAccessor propertiesAccessor,
            PanacheCrudResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        MethodCreator methodCreator = classCreator
                .getMethodCreator(methodMetadata.getName(), void.class.getName(), methodMetadata.getParameterTypes());
        addTransactionalAnnotation(methodCreator);
        addDeleteAnnotation(methodCreator);
        addPathAnnotation(methodCreator,
                propertiesAccessor.getPath(resourceInfo.getResourceClassInfo(), methodMetadata, "{id}"));
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addLinksAnnotation(methodCreator, resourceInfo.getEntityClassName(), REL);

        ResultHandle result = resourceInfo.getDataAccessImplementor()
                .deleteById(methodCreator, methodCreator.getMethodParam(0));
        BranchResult entityWasDeleted = methodCreator.ifNonZero(result);

        entityWasDeleted.trueBranch().returnValue(null);
        entityWasDeleted.falseBranch().throwException(ResponseImplementor.notFoundException(entityWasDeleted.falseBranch()));
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getMethodMetadata(PanacheCrudResourceInfo resourceInfo) {
        return new MethodMetadata(NAME, resourceInfo.getIdClassName());
    }
}
