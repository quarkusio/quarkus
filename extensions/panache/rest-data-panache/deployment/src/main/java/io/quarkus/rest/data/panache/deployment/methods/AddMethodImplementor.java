package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.param;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.responseType;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.uniType;

import jakarta.validation.Valid;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestResponse;

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

public final class AddMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "add";

    private static final String RESOURCE_METHOD_NAME = "add";

    private static final String EXCEPTION_MESSAGE = "Failed to add an entity";

    private static final String REL = "add";

    public AddMethodImplementor(Capabilities capabilities) {
        super(capabilities);
    }

    /**
     * Generate JAX-RS POST method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#add(Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;POST
     *     &#64;Path("")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "add",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Response add(Entity entityToSave) {
     *         try {
     *             Entity entity = restDataResource.add(entityToSave);
     *             String location = new ResourceLinksProvider().getSelfLink(entity);
     *             if (location != null) {
     *                 ResponseBuilder responseBuilder = Response.status(201);
     *                 responseBuilder.entity(entity);
     *                 responseBuilder.location(URI.create(location));
     *                 return responseBuilder.build();
     *             } else {
     *                 throw new RuntimeException("Could not extract a new entity URL")
     *             }
     *         } catch (Throwable t) {
     *             throw new RestDataPanacheException(t);
     *         }
     *     }
     * }
     * </pre>
     *
     * The RESTEasy Reactive version exposes {@link io.quarkus.rest.data.panache.ReactiveRestDataResource#add(Object)}
     * and the generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;POST
     *     &#64;Path("")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "add",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Uni<Response> add(Entity entityToSave) {
     *         return restDataResource.add(entityToSave).map(entity -> {
     *             String location = new ResourceLinksProvider().getSelfLink(entity);
     *             if (location != null) {
     *                 ResponseBuilder responseBuilder = Response.status(201);
     *                 responseBuilder.entity(entity);
     *                 responseBuilder.location(URI.create(location));
     *                 return responseBuilder.build();
     *             } else {
     *                 throw new RuntimeException("Could not extract a new entity URL")
     *             }
     *         }).onFailure().invoke(t -> throw new RestDataPanacheException(t));
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = SignatureMethodCreator.getMethodCreator(METHOD_NAME, classCreator,
                isNotReactivePanache() ? responseType(resourceMetadata.getEntityType())
                        : uniType(resourceMetadata.getEntityType()),
                param("entity", resourceMetadata.getEntityType()), param("uriInfo", UriInfo.class));

        // Add method annotations
        addPathAnnotation(methodCreator, resourceProperties.getPath(RESOURCE_METHOD_NAME));
        addMethodAnnotations(methodCreator, resourceProperties.getMethodAnnotations(RESOURCE_METHOD_NAME));
        addPostAnnotation(methodCreator);
        addContextAnnotation(methodCreator.getParameterAnnotations(1));
        addConsumesAnnotation(methodCreator, APPLICATION_JSON);
        addProducesJsonAnnotation(methodCreator, resourceProperties);
        addLinksAnnotation(methodCreator, resourceProperties, resourceMetadata.getEntityType(), REL);
        addOpenApiResponseAnnotation(methodCreator, RestResponse.Status.CREATED, resourceMetadata.getEntityType());
        addSecurityAnnotations(methodCreator, resourceProperties);
        // Add parameter annotations
        if (hasValidatorCapability()) {
            methodCreator.getParameterAnnotations(0).addAnnotation(Valid.class);
        }

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle entityToSave = methodCreator.getMethodParam(0);

        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle entity = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Object.class, Object.class),
                    resource, entityToSave);
            tryBlock.returnValue(responseImplementor.created(tryBlock, entity, resourceProperties));
            tryBlock.close();
        } else {
            ResultHandle uniEntity = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Uni.class, Object.class),
                    resource, entityToSave);

            methodCreator.returnValue(UniImplementor.map(methodCreator, uniEntity, EXCEPTION_MESSAGE,
                    (body, item) -> body.returnValue(responseImplementor.created(body, item, resourceProperties))));
        }

        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
