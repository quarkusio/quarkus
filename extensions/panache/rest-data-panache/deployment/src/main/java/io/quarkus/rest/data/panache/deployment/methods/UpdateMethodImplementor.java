package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.param;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.responseType;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.uniType;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import jakarta.validation.Valid;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.deployment.Capabilities;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;
import io.quarkus.rest.data.panache.runtime.UpdateExecutor;
import io.smallrye.mutiny.Uni;

public final class UpdateMethodImplementor extends StandardMethodImplementor {

    private static final String METHOD_NAME = "update";

    private static final String RESOURCE_UPDATE_METHOD_NAME = "update";

    private static final String RESOURCE_GET_METHOD_NAME = "get";

    private static final String REL = "update";

    private static final String EXCEPTION_MESSAGE = "Failed to update an entity";

    public UpdateMethodImplementor(Capabilities capabilities) {
        super(capabilities);
    }

    /**
     * Generate JAX-RS UPDATE method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#update(Object, Object)}.
     * Expose {@link RestDataResource#update(Object, Object)} via JAX-RS method.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;PUT
     *     &#64;Path("{id}")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "update",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Response update(@PathParam("id") ID id, Entity entityToSave) {
     *         try {
     *             Object newEntity = updateExecutor.execute(() -> {
     *                 if (resource.get(id) == null) {
     *                     return resource.update(id, entityToSave);
     *                 } else {
     *                     resource.update(id, entityToSave);
     *                     return null;
     *                 }
     *             });
     *
     *             if (newEntity == null) {
     *                 return Response.status(204).build();
     *             } else {
     *                 String location = new ResourceLinksProvider().getSelfLink(newEntity);
     *                 if (location != null) {
     *                     ResponseBuilder responseBuilder = Response.status(201);
     *                     responseBuilder.entity(newEntity);
     *                     responseBuilder.location(URI.create(location));
     *                     return responseBuilder.build();
     *                 } else {
     *                     throw new RuntimeException("Could not extract a new entity URL")
     *                 }
     *             }
     *         } catch (Throwable t) {
     *             throw new RestDataPanacheException(t);
     *         }
     *     }
     * }
     * </pre>
     *
     * The RESTEasy Reactive version exposes
     * {@link io.quarkus.rest.data.panache.ReactiveRestDataResource#update(Object, Object)}.
     *
     * <pre>
     * {@code
     *     &#64;PUT
     *     &#64;Path("{id}")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "update",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Uni<Response> update(@PathParam("id") ID id, Entity entityToSave) {
     *         return resource.get(id).flatMap(entity -> {
     *             if (entity == null) {
     *                 return Uni.createFrom().item(Response.status(204).build());
     *             } else {
     *                 return resource.update(id, entityToSave).map(savedEntity -> {
     *                     String location = new ResourceLinksProvider().getSelfLink(savedEntity);
     *                     if (location != null) {
     *                         ResponseBuilder responseBuilder = Response.status(201);
     *                         responseBuilder.entity(savedEntity);
     *                         responseBuilder.location(URI.create(location));
     *                         return responseBuilder.build();
     *                     } else {
     *                         throw new RuntimeException("Could not extract a new entity URL")
     *                     }
     *                 });
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
                param("id", resourceMetadata.getIdType()),
                param("entity", resourceMetadata.getEntityType()),
                param("uriInfo", UriInfo.class));

        // Add method annotations
        addPathAnnotation(methodCreator, appendToPath(resourceProperties.getPath(RESOURCE_UPDATE_METHOD_NAME), "{id}"));
        addPutAnnotation(methodCreator);
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addContextAnnotation(methodCreator.getParameterAnnotations(2));
        addConsumesAnnotation(methodCreator, APPLICATION_JSON);
        addProducesJsonAnnotation(methodCreator, resourceProperties);
        addLinksAnnotation(methodCreator, resourceProperties, resourceMetadata.getEntityType(), REL);
        addMethodAnnotations(methodCreator, resourceProperties.getMethodAnnotations(RESOURCE_UPDATE_METHOD_NAME));
        addOpenApiResponseAnnotation(methodCreator, RestResponse.Status.CREATED, resourceMetadata.getEntityType());
        addSecurityAnnotations(methodCreator, resourceProperties);
        // Add parameter annotations
        if (hasValidatorCapability()) {
            methodCreator.getParameterAnnotations(1).addAnnotation(Valid.class);
        }

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entityToSave = methodCreator.getMethodParam(1);

        if (isNotReactivePanache()) {
            implementClassicVersion(methodCreator, resourceMetadata, resourceProperties, resource, id, entityToSave);
        } else {
            implementReactiveVersion(methodCreator, resourceMetadata, resourceProperties, resource, id, entityToSave);
        }

        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_UPDATE_METHOD_NAME;
    }

    private void implementReactiveVersion(MethodCreator methodCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, ResultHandle resource, ResultHandle id, ResultHandle entityToSave) {
        ResultHandle uniResponse = methodCreator.invokeVirtualMethod(
                ofMethod(resourceMetadata.getResourceClass(), RESOURCE_GET_METHOD_NAME, Uni.class, Object.class),
                resource, id);

        methodCreator
                .returnValue(UniImplementor.flatMap(methodCreator, uniResponse, EXCEPTION_MESSAGE, (getBody, itemWasFound) -> {

                    ResultHandle uniUpdateEntity = getBody.invokeVirtualMethod(
                            ofMethod(resourceMetadata.getResourceClass(), RESOURCE_UPDATE_METHOD_NAME, Uni.class, Object.class,
                                    Object.class),
                            resource, id, entityToSave);

                    getBody.returnValue(UniImplementor.map(getBody, uniUpdateEntity, EXCEPTION_MESSAGE,
                            (updateBody, itemUpdated) -> {
                                BranchResult ifEntityIsNew = updateBody.ifNull(itemWasFound);
                                ifEntityIsNew.trueBranch()
                                        .returnValue(responseImplementor.created(ifEntityIsNew.trueBranch(), itemUpdated,
                                                resourceProperties));
                                ifEntityIsNew.falseBranch()
                                        .returnValue(responseImplementor.noContent(ifEntityIsNew.falseBranch()));
                            }));
                }));
    }

    private void implementClassicVersion(MethodCreator methodCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, ResultHandle resource, ResultHandle id, ResultHandle entityToSave) {
        // Invoke resource methods inside a supplier function which will be given to an update executor.
        // For ORM, this update executor will have the @Transactional annotation to make
        // sure that all database operations are executed in a single transaction.
        TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
        ResultHandle updateExecutor = getUpdateExecutor(tryBlock);
        ResultHandle updateFunction = getUpdateFunction(tryBlock, resourceMetadata.getResourceClass(), resource, id,
                entityToSave);
        ResultHandle newEntity = tryBlock.invokeInterfaceMethod(
                ofMethod(UpdateExecutor.class, "execute", Object.class, Supplier.class),
                updateExecutor, updateFunction);

        BranchResult createdNewEntity = tryBlock.ifNotNull(newEntity);
        createdNewEntity.trueBranch()
                .returnValue(responseImplementor.created(createdNewEntity.trueBranch(), newEntity, resourceProperties));
        createdNewEntity.falseBranch().returnValue(responseImplementor.noContent(createdNewEntity.falseBranch()));
    }

    private ResultHandle getUpdateFunction(BytecodeCreator creator, String resourceClass, ResultHandle resource,
            ResultHandle id, ResultHandle entity) {
        FunctionCreator functionCreator = creator.createFunction(Supplier.class);
        BytecodeCreator functionBytecodeCreator = functionCreator.getBytecode();

        AssignableResultHandle entityToSave = functionBytecodeCreator.createVariable(Object.class);
        functionBytecodeCreator.assign(entityToSave, entity);

        BranchResult shouldUpdate = entityExists(functionBytecodeCreator, resourceClass, resource, id);
        // Update and return null
        updateAndReturn(shouldUpdate.trueBranch(), resourceClass, resource, id, entityToSave);
        // Update and return new entity
        createAndReturn(shouldUpdate.falseBranch(), resourceClass, resource, id, entityToSave);

        return functionCreator.getInstance();
    }

    private BranchResult entityExists(BytecodeCreator creator, String resourceClass, ResultHandle resource,
            ResultHandle id) {
        return creator.ifNotNull(creator.invokeVirtualMethod(
                ofMethod(resourceClass, RESOURCE_GET_METHOD_NAME, Object.class, Object.class), resource, id));
    }

    private void createAndReturn(BytecodeCreator creator, String resourceClass, ResultHandle resource,
            ResultHandle id, ResultHandle entityToSave) {
        ResultHandle newEntity = creator.invokeVirtualMethod(
                ofMethod(resourceClass, RESOURCE_UPDATE_METHOD_NAME, Object.class, Object.class, Object.class),
                resource, id, entityToSave);
        creator.returnValue(newEntity);
    }

    private void updateAndReturn(BytecodeCreator creator, String resourceClass, ResultHandle resource,
            ResultHandle id, ResultHandle entityToSave) {
        creator.invokeVirtualMethod(
                ofMethod(resourceClass, RESOURCE_UPDATE_METHOD_NAME, Object.class, Object.class, Object.class),
                resource, id, entityToSave);
        creator.returnValue(creator.loadNull());
    }

    private ResultHandle getUpdateExecutor(BytecodeCreator creator) {
        ResultHandle arcContainer = creator.invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = creator.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class),
                arcContainer, creator.loadClassFromTCCL(UpdateExecutor.class), creator.newArray(Annotation.class, 0));
        ResultHandle instance = creator.invokeInterfaceMethod(
                ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);

        creator.ifNull(instance)
                .trueBranch()
                .throwException(RuntimeException.class,
                        UpdateExecutor.class.getSimpleName() + " instance was not found");

        return instance;
    }
}
