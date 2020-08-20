package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;

public final class DeleteMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "delete";

    private static final String RESOURCE_METHOD_NAME = "delete";

    private static final String REL = "remove";

    /**
     * Generate JAX-RS DELETE method that exposes {@link RestDataResource#delete(Object)}.
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
     *         if (!restDataResource.delete(id)) {
     *             throw new WebApplicationException(404);
     *         }
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = classCreator.getMethodCreator(METHOD_NAME, void.class.getName(),
                resourceMetadata.getIdType());

        // Add method annotations
        addPathAnnotation(methodCreator, appendToPath(resourceProperties.getMethodPath(RESOURCE_METHOD_NAME), "{id}"));
        addTransactionalAnnotation(methodCreator);
        addDeleteAnnotation(methodCreator);
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), REL);

        // Invoke resource methods
        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle result = methodCreator.invokeVirtualMethod(
                ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, boolean.class, Object.class),
                resource, id);
        BranchResult entityWasDeleted = methodCreator.ifNonZero(result);

        // Return response
        entityWasDeleted.trueBranch().returnValue(null);
        entityWasDeleted.falseBranch()
                .throwException(ResponseImplementor.notFoundException(entityWasDeleted.falseBranch()));
        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
