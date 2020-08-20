package io.quarkus.rest.data.panache.deployment.methods.hal;

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
import io.quarkus.rest.data.panache.runtime.hal.HalEntityWrapper;

public final class GetHalMethodImplementor extends HalMethodImplementor {

    private static final String METHOD_NAME = "getHal";

    private static final String RESOURCE_METHOD_NAME = "get";

    /**
     * Expose {@link RestDataResource#get(Object)} via HAL JAX-RS method.
     * Generated code looks more or less like this:
     * 
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Produces({"application/hal+json"})
     *     &#64;Path("{id}")
     *     public HalEntityWrapper getHal(@PathParam("id") ID id) {
     *         Entity entity = resource.get(id);
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
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = classCreator.getMethodCreator(METHOD_NAME, HalEntityWrapper.class,
                resourceMetadata.getIdType());

        // Add method annotations
        addPathAnnotation(methodCreator, appendToPath(resourceProperties.getMethodPath(RESOURCE_METHOD_NAME), "{id}"));
        addGetAnnotation(methodCreator);
        addProducesAnnotation(methodCreator, APPLICATION_HAL_JSON);
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");

        // Invoke resource methods
        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entity = methodCreator.invokeVirtualMethod(
                ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Object.class, Object.class),
                resource, id);
        BranchResult entityNotFound = methodCreator.ifNull(entity);

        // Wrap and return response
        entityNotFound.trueBranch().throwException(ResponseImplementor.notFoundException(entityNotFound.trueBranch()));
        entityNotFound.falseBranch().returnValue(wrapHalEntity(entityNotFound.falseBranch(), entity));
        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
