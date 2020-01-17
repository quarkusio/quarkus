package io.quarkus.panache.rest.common.deployment.methods;

import javax.ws.rs.core.Response;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.DataAccessImplementor;
import io.quarkus.panache.rest.common.deployment.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.utils.FieldAccessImplementor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceAnnotator;
import io.quarkus.panache.rest.common.deployment.utils.ResponseImplementor;
import io.quarkus.panache.rest.common.deployment.utils.UrlImplementor;
import io.quarkus.panache.rest.common.runtime.hal.HalEntityWrapper;

public final class UpdateHalMethodImplementor implements MethodImplementor {

    private final DataAccessImplementor dataAccessImplementor;

    private final FieldAccessImplementor fieldAccessor;

    private final UrlImplementor urlProvider;

    private final String idClassName;

    private final String entityClassName;

    public UpdateHalMethodImplementor(DataAccessImplementor dataAccessImplementor, FieldAccessImplementor fieldAccessor,
            UrlImplementor urlProvider, String idClassName, String entityClassName) {
        this.dataAccessImplementor = dataAccessImplementor;
        this.fieldAccessor = fieldAccessor;
        this.urlProvider = urlProvider;
        this.idClassName = idClassName;
        this.entityClassName = entityClassName;
    }

    /**
     * Implements HAL version of {@link PanacheCrudResource#update(Object, Object)}.
     * Generated code looks more or less like this:
     * 
     * <pre>
     * {@code
     *     &#64;Transactional
     *     &#64;PUT
     *     &#64;Path("entities/{id}")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/hal+json"})
     *     public Response updateHal(@PathParam("id") ID id, Entity entity) {
     *         if (BookEntity.findById(id) != null) {
     *             entity.id = id;
     *             JpaOperations.getEntityManager().merge(entity);
     *             return Response.status(204).build();
     *         } else {
     *             entity.id = id;
     *             Object newEntity = JpaOperations.getEntityManager().merge(entity);
     *             String newId = String.valueOf(newEntity.id);
     *             CharSequence[] pathElements = new CharSequence[]{"entities", newId};
     *             URI uri = URI.create(String.join((CharSequence)"/", pathElements));
     *             HalEntityWrapper wrappedEntity = new HalEntityWrapper(newEntity);
     *             ResponseBuilder responseBuilder = Response.status(201);
     *             responseBuilder.entity(wrappedEntity);
     *             responseBuilder.location(uri);
     *             return responseBuilder.build();
     *         }
     *     }
     * }
     * </pre>
     *
     * @param classCreator
     */
    @Override
    public void implement(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator
                .getMethodCreator("updateHal", Response.class, idClassName, entityClassName);
        ResourceAnnotator.addTransactional(methodCreator);
        ResourceAnnotator.addPut(methodCreator);
        ResourceAnnotator.addPath(methodCreator, UrlImplementor.getCollectionUrl(entityClassName) + "/{id}");
        ResourceAnnotator.addPathParam(methodCreator.getParameterAnnotations(0), "id");
        ResourceAnnotator.addConsumes(methodCreator, ResourceAnnotator.APPLICATION_JSON);
        ResourceAnnotator.addProduces(methodCreator, ResourceAnnotator.APPLICATION_HAL_JSON);

        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entity = methodCreator.getMethodParam(1);
        BranchResult entityDoesNotExist = methodCreator.ifNull(dataAccessImplementor.findById(methodCreator, id));

        createAndReturn(entityDoesNotExist.trueBranch(), entityClassName, entity, id);
        updateAndReturn(entityDoesNotExist.falseBranch(), entityClassName, entity, id);
        methodCreator.close();
    }

    private void createAndReturn(BytecodeCreator creator, String entityType, ResultHandle entity, ResultHandle id) {
        fieldAccessor.setId(creator, entityType, entity, id);
        ResultHandle newEntity = dataAccessImplementor.update(creator, entity);
        ResultHandle url = urlProvider.getEntityUrl(creator, newEntity, entityType);
        ResultHandle response = ResponseImplementor.created(creator, wrapHalEntity(creator, newEntity), url);
        creator.returnValue(response);
    }

    private void updateAndReturn(BytecodeCreator creator, String entityType, ResultHandle entity, ResultHandle id) {
        fieldAccessor.setId(creator, entityType, entity, id);
        dataAccessImplementor.update(creator, entity);
        creator.returnValue(ResponseImplementor.noContent(creator));
    }

    private ResultHandle wrapHalEntity(BytecodeCreator creator, ResultHandle entity) {
        return creator.newInstance(MethodDescriptor.ofConstructor(HalEntityWrapper.class, Object.class), entity);
    }
}
