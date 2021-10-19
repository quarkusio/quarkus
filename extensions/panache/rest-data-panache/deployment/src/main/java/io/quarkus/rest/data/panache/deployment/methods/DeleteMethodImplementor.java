package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import javax.ws.rs.core.Response;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;

public final class DeleteMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "delete";

    private static final String RESOURCE_METHOD_NAME = "delete";

    private static final String REL = "remove";

    public DeleteMethodImplementor(boolean isResteasyClassic) {
        super(isResteasyClassic);
    }

    /**
     * Generate JAX-RS DELETE method that exposes {@link RestDataResource#delete(Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;DELETE
     *     &#64;Path("{id}")
     *     &#64;LinkResource(
     *         rel = "remove",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Response delete(@PathParam("id") ID id) {
     *         try {
     *             boolean deleted = restDataResource.delete(id);
     *             if (deleted) {
     *                 return Response.noContent().build();
     *             } else {
     *                 return Response.status(404).build();
     *             }
     *         } catch (Throwable t) {
     *             throw new RestDataPanacheException(t);
     *         }
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = classCreator.getMethodCreator(METHOD_NAME, Response.class,
                resourceMetadata.getIdType());

        // Add method annotations
        addPathAnnotation(methodCreator, appendToPath(resourceProperties.getPath(RESOURCE_METHOD_NAME), "{id}"));
        addDeleteAnnotation(methodCreator);
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), REL);

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);

        // Invoke resource methods
        TryBlock tryBlock = implementTryBlock(methodCreator, "Failed to delete an entity");
        ResultHandle deleted = tryBlock.invokeVirtualMethod(
                ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, boolean.class, Object.class),
                resource, id);

        // Return response
        BranchResult entityWasDeleted = tryBlock.ifNonZero(deleted);
        entityWasDeleted.trueBranch().returnValue(ResponseImplementor.noContent(entityWasDeleted.trueBranch()));
        entityWasDeleted.falseBranch().returnValue(ResponseImplementor.notFound(entityWasDeleted.falseBranch()));

        tryBlock.close();
        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
