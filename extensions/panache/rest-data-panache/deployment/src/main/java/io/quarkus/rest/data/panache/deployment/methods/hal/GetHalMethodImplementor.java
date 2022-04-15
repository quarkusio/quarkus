package io.quarkus.rest.data.panache.deployment.methods.hal;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import javax.ws.rs.core.Response;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;
import io.smallrye.mutiny.Uni;

public final class GetHalMethodImplementor extends HalMethodImplementor {

    private static final String METHOD_NAME = "getHal";

    private static final String RESOURCE_METHOD_NAME = "get";

    private static final String EXCEPTION_MESSAGE = "Failed to get an entity";

    public GetHalMethodImplementor(boolean isResteasyClassic, boolean isReactivePanache) {
        super(isResteasyClassic, isReactivePanache);
    }

    /**
     * Generate HAL JAX-RS GET method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#get(Object)} via HAL JAX-RS method.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Produces({"application/hal+json"})
     *     &#64;Path("{id}")
     *     public Response getHal(@PathParam("id") ID id) {
     *         try {
     *             Entity entity = resource.get(id);
     *             if (entity != null) {
     *                 return Response.ok(new HalEntityWrapper(entity)).build();
     *             } else {
     *                 return Response.status(404).build();
     *             }
     *         } catch (Throwable t) {
     *             throw new RestDataPanacheException(t);
     *         }
     *     }
     * }
     * </pre>
     *
     * The RESTEasy Reactive version exposes {@link io.quarkus.rest.data.panache.ReactiveRestDataResource#get(Object)}
     * and the generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Produces({"application/hal+json"})
     *     &#64;Path("{id}")
     *     public Uni<Response> getHal(@PathParam("id") ID id) {
     *         return resource.get(id).map(entity -> {
     *             if (entity != null) {
     *                 return Response.ok(new HalEntityWrapper(entity)).build();
     *             } else {
     *                 return Response.status(404).build();
     *             }
     *         }).onFailure().invoke(t -> throw new RestDataPanacheException(t));
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = classCreator.getMethodCreator(METHOD_NAME,
                isNotReactivePanache() ? Response.class : Uni.class,
                resourceMetadata.getIdType());

        // Add method annotations
        addPathAnnotation(methodCreator, appendToPath(resourceProperties.getPath(RESOURCE_METHOD_NAME), "{id}"));
        addGetAnnotation(methodCreator);
        addProducesAnnotation(methodCreator, APPLICATION_HAL_JSON);
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle entity = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Object.class, Object.class),
                    resource, id);

            // Wrap and return response
            ifNullReturnNotFound(tryBlock, entity);

            tryBlock.close();
        } else {
            ResultHandle uniEntity = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Uni.class, Object.class),
                    resource, id);

            methodCreator.returnValue(UniImplementor.map(methodCreator, uniEntity, EXCEPTION_MESSAGE,
                    (body, entity) -> ifNullReturnNotFound(body, entity)));
        }

        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }

    private void ifNullReturnNotFound(BytecodeCreator body, ResultHandle entity) {
        BranchResult wasNotFound = body.ifNull(entity);
        wasNotFound.trueBranch().returnValue(ResponseImplementor.notFound(wasNotFound.trueBranch()));
        wasNotFound.falseBranch().returnValue(
                ResponseImplementor.ok(wasNotFound.falseBranch(), wrapHalEntity(wasNotFound.falseBranch(), entity)));
    }
}
