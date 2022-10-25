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

public final class DeleteMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "delete";

    private static final String RESOURCE_METHOD_NAME = "delete";

    private static final String EXCEPTION_MESSAGE = "Failed to delete an entity";

    private static final String REL = "remove";

    public DeleteMethodImplementor(boolean isResteasyClassic, boolean isReactivePanache) {
        super(isResteasyClassic, isReactivePanache);
    }

    /**
     * Generate JAX-RS DELETE method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#delete(Object)}
     * and the generated code looks more or less like this:
     *
     * <pre>
     * {@code
     * &#64;DELETE
     * &#64;Path("{id}")
     * &#64;LinkResource(rel = "remove", entityClassName = "com.example.Entity")
     * public Response delete(@PathParam("id") ID id) throws RestDataPanacheException {
     *     try {
     *         boolean deleted = restDataResource.delete(id);
     *         if (deleted) {
     *             return Response.noContent().build();
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
     * &#64;DELETE
     * &#64;Path("{id}")
     * &#64;LinkResource(rel = "remove", entityClassName = "com.example.Entity")
     * public Uni<Response> delete(@PathParam("id") ID id) throws RestDataPanacheException {
     *     try {
     *         return restDataResource.delete(id)
     *                 .map(deleted -> deleted ? Response.noContent().build() : Response.status(404).build());
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
        addDeleteAnnotation(methodCreator);
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), REL);

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle deleted = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, boolean.class, Object.class),
                    resource, id);

            // Return response
            BranchResult entityWasDeleted = tryBlock.ifNonZero(deleted);
            entityWasDeleted.trueBranch().returnValue(responseImplementor.noContent(entityWasDeleted.trueBranch()));
            entityWasDeleted.falseBranch().returnValue(responseImplementor.notFound(entityWasDeleted.falseBranch()));

            tryBlock.close();
        } else {
            ResultHandle uniDeleted = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Uni.class, Object.class),
                    resource, id);

            methodCreator.returnValue(UniImplementor.map(methodCreator, uniDeleted, EXCEPTION_MESSAGE,
                    (body, entity) -> {
                        ResultHandle deleted = body.checkCast(entity, Boolean.class);
                        // Workaround to have boolean type, otherwise it's an integer.
                        ResultHandle falseDefault = body.invokeStaticMethod(
                                ofMethod(Boolean.class, "valueOf", Boolean.class, String.class), body.load("false"));
                        ResultHandle deletedAsInt = body.invokeVirtualMethod(
                                ofMethod(Boolean.class, "compareTo", int.class, Boolean.class), deleted, falseDefault);

                        BranchResult entityWasDeleted = body.ifNonZero(deletedAsInt);
                        entityWasDeleted.trueBranch()
                                .returnValue(responseImplementor.noContent(entityWasDeleted.trueBranch()));
                        entityWasDeleted.falseBranch()
                                .returnValue(responseImplementor.notFound(entityWasDeleted.falseBranch()));
                    }));
        }

        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
