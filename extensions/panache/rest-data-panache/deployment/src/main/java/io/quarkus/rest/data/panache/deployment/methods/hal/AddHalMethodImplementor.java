package io.quarkus.rest.data.panache.deployment.methods.hal;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import javax.validation.Valid;
import javax.ws.rs.core.Response;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;

public final class AddHalMethodImplementor extends HalMethodImplementor {

    private static final String METHOD_NAME = "addHal";

    private static final String RESOURCE_METHOD_NAME = "add";

    private final boolean withValidation;

    public AddHalMethodImplementor(boolean withValidation, boolean isResteasyClassic) {
        super(isResteasyClassic);
        this.withValidation = withValidation;
    }

    /**
     * Expose {@link RestDataResource#add(Object)} via HAL JAX-RS method.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;POST
     *     &#64;Path("")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/hal+json"})
     *     public Response addHal(Entity entityToSave) {
     *         try {
     *             Entity entity = resource.add(entityToSave);
     *             HalEntityWrapper wrapper = new HalEntityWrapper(entity);
     *             String location = new ResourceLinksProvider().getSelfLink(entity);
     *             if (location != null) {
     *                 ResponseBuilder responseBuilder = Response.status(201);
     *                 responseBuilder.entity(wrapper);
     *                 responseBuilder.location(URI.create(location));
     *                 return responseBuilder.build();
     *             } else {
     *                 throw new RuntimeException("Could not extract a new entity URL");
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
                resourceMetadata.getEntityType());

        // Add method annotations
        addPathAnnotation(methodCreator, resourceProperties.getPath(RESOURCE_METHOD_NAME));
        addPostAnnotation(methodCreator);
        addConsumesAnnotation(methodCreator, APPLICATION_JSON);
        addProducesAnnotation(methodCreator, APPLICATION_HAL_JSON);
        // Add parameter annotations
        if (withValidation) {
            methodCreator.getParameterAnnotations(0).addAnnotation(Valid.class);
        }

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle entityToSave = methodCreator.getMethodParam(0);

        // Invoke resource methods
        TryBlock tryBlock = implementTryBlock(methodCreator, "Failed to add an entity");
        ResultHandle entity = tryBlock.invokeVirtualMethod(
                ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Object.class, Object.class),
                resource, entityToSave);

        // Wrap and return response
        tryBlock.returnValue(ResponseImplementor.created(tryBlock, wrapHalEntity(tryBlock, entity),
                ResponseImplementor.getEntityUrl(tryBlock, entity)));

        tryBlock.close();
        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
