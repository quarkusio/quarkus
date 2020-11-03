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

public final class GetMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "get";

    private static final String RESOURCE_METHOD_NAME = "get";

    private static final String REL = "self";

    /**
     * Generate JAX-RS GET method that exposes {@link RestDataResource#get(Object)}.
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
     *         Entity entity = restDataResource.get(id);
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
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = classCreator.getMethodCreator(METHOD_NAME, resourceMetadata.getEntityType(),
                resourceMetadata.getIdType());

        // Add method annotations
        addPathAnnotation(methodCreator, appendToPath(resourceProperties.getMethodPath(RESOURCE_METHOD_NAME), "{id}"));
        addGetAnnotation(methodCreator);
        addProducesAnnotation(methodCreator, APPLICATION_JSON);
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), REL);

        // Invoke resource methods
        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entity = methodCreator.invokeVirtualMethod(
                ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Object.class, Object.class),
                resource, id);
        BranchResult entityNotFound = methodCreator.ifNull(entity);

        // Return response
        entityNotFound.trueBranch().throwException(ResponseImplementor.notFoundException(entityNotFound.trueBranch()));
        entityNotFound.falseBranch().returnValue(entity);
        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
