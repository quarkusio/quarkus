package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.ofType;

import jakarta.ws.rs.core.Response;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;
import io.smallrye.mutiny.Uni;

public final class GetMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "get";

    private static final String RESOURCE_METHOD_NAME = "get";

    private static final String EXCEPTION_MESSAGE = "Failed to get an entity";

    private static final String REL = "self";

    public GetMethodImplementor(boolean isResteasyClassic, boolean isReactivePanache) {
        super(isResteasyClassic, isReactivePanache);
    }

    /**
     * Generate JAX-RS GET method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#get(Object)}.
     * and the generated code looks more or less like this:
     *
     * <pre>
     * {@code
     * &#64;GET
     * &#64;Produces({ "application/json" })
     * &#64;Path("{id}")
     * &#64;LinkResource(rel = "self", entityClassName = "com.example.Entity")
     * public Response get(@PathParam("id") ID id) {
     *     try {
     *         Entity entity = restDataResource.get(id);
     *         if (entity != null) {
     *             return entity;
     *         } else {
     *             return Response.status(404).build();
     *         }
     *     } catch (Throwable t) {
     *         throw new RestDataPanacheException(t);
     *     }
     * }
     * }
     * </pre>
     *
     * The RESTEasy Reactive version exposes {@link io.quarkus.rest.data.panache.ReactiveRestDataResource#delete(Object)}
     * and the generated code looks more or less like this:
     *
     * <pre>
     * {@code
     * &#64;GET
     * &#64;Produces({ "application/json" })
     * &#64;Path("{id}")
     * &#64;LinkResource(rel = "self", entityClassName = "com.example.Entity")
     * public Uni<Response> get(@PathParam("id") ID id) {
     *     try {
     *         return restDataResource.get(id)
     *                 .map(entity -> entity == null ? Response.status(404).build() : Response.ok(entity).build());
     *     } catch (Throwable t) {
     *         throw new RestDataPanacheException(t);
     *     }
     * }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = SignatureMethodCreator.getMethodCreator(METHOD_NAME, classCreator,
                isNotReactivePanache() ? ofType(Response.class) : ofType(Uni.class, resourceMetadata.getEntityType()),
                resourceMetadata.getIdType());

        // Add method annotations
        addPathAnnotation(methodCreator, appendToPath(resourceProperties.getPath(RESOURCE_METHOD_NAME), "{id}"));
        addGetAnnotation(methodCreator);
        addProducesJsonAnnotation(methodCreator, resourceProperties);

        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), REL);

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle entity = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Object.class, Object.class),
                    resource, id);

            // Return response
            BranchResult wasNotFound = tryBlock.ifNull(entity);
            wasNotFound.trueBranch().returnValue(responseImplementor.notFound(wasNotFound.trueBranch()));
            wasNotFound.falseBranch().returnValue(responseImplementor.ok(wasNotFound.falseBranch(), entity));

            tryBlock.close();
        } else {
            ResultHandle uniEntity = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Uni.class, Object.class),
                    resource, id);

            methodCreator.returnValue(UniImplementor.map(methodCreator, uniEntity, EXCEPTION_MESSAGE,
                    (body, entity) -> {
                        BranchResult entityWasNotFound = body.ifNull(entity);
                        entityWasNotFound.trueBranch()
                                .returnValue(responseImplementor.notFound(entityWasNotFound.trueBranch()));
                        entityWasNotFound.falseBranch()
                                .returnValue(responseImplementor.ok(entityWasNotFound.falseBranch(), entity));
                    }));
        }

        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
