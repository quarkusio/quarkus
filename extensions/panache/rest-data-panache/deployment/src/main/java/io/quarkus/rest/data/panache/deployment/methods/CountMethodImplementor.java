package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.ofType;

import jakarta.ws.rs.core.Response;

import io.quarkus.deployment.Capabilities;
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

public final class CountMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "count";

    private static final String RESOURCE_METHOD_NAME = "count";

    private static final String EXCEPTION_MESSAGE = "Failed to count the entities";

    private static final String REL = "count";

    public CountMethodImplementor(Capabilities capabilities) {
        super(capabilities);
    }

    /**
     * Generate JAX-RS GET method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#count()}.
     *
     * <pre>
     * {@code
     * &#64;GET
     * &#64;Path("/count")
     * public long count() {
     *     try {
     *         return resource.count();
     *     } catch (Throwable t) {
     *         throw new RestDataPanacheException(t);
     *     }
     * }
     * }
     * </pre>
     *
     * The RESTEasy Reactive version exposes {@link io.quarkus.rest.data.panache.ReactiveRestDataResource#count()}
     * and the generated code looks more or less like this:
     *
     * <pre>
     * {@code
     * &#64;GET
     * &#64;Path("/count")
     * &#64;LinkResource(rel = "count", entityClassName = "com.example.Entity")
     * public Uni<Long> count() {
     *     try {
     *         return resource.count();
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
        // Method parameters: sort strings, page index, page size, uri info
        MethodCreator methodCreator = SignatureMethodCreator.getMethodCreator(RESOURCE_METHOD_NAME, classCreator,
                isNotReactivePanache() ? ofType(Response.class) : ofType(Uni.class, Long.class));

        // Add method annotations
        addGetAnnotation(methodCreator);
        addProducesAnnotation(methodCreator, APPLICATION_JSON);
        addPathAnnotation(methodCreator, appendToPath(resourceProperties.getPath(RESOURCE_METHOD_NAME), RESOURCE_METHOD_NAME));
        addMethodAnnotations(methodCreator, resourceProperties.getMethodAnnotations(RESOURCE_METHOD_NAME));
        addOpenApiResponseAnnotation(methodCreator, Response.Status.OK, Long.class, false);
        addSecurityAnnotations(methodCreator, resourceProperties);
        if (!isResteasyClassic()) {
            // We only add the Links annotation in Resteasy Reactive because Resteasy Classic ignores the REL parameter:
            // it always uses "list" for GET methods, so it interferes with the list implementation.
            addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), REL);
        }

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle count = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), METHOD_NAME, long.class),
                    resource);

            // Return response
            tryBlock.returnValue(responseImplementor.ok(tryBlock, count));
            tryBlock.close();
        } else {
            ResultHandle uniCount = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), METHOD_NAME, Uni.class),
                    resource);
            methodCreator.returnValue(UniImplementor.map(methodCreator, uniCount, EXCEPTION_MESSAGE,
                    (countBody, count) -> countBody.returnValue(responseImplementor.ok(countBody, count))));
        }

        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
