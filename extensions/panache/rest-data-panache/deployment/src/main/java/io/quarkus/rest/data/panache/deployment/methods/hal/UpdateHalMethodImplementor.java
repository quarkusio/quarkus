package io.quarkus.rest.data.panache.deployment.methods.hal;

import javax.ws.rs.core.Response;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.methods.MethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodMetadata;
import io.quarkus.rest.data.panache.deployment.methods.UpdateMethodImplementor;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.utils.FieldAccessImplementor;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;

public final class UpdateHalMethodImplementor extends HalMethodImplementor {

    private static final String NAME = "updateHal";

    /**
     * Implements HAL version of {@link RestDataResource#update(Object, Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;Transactional
     *     &#64;PUT
     *     &#64;Path("{id}")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/hal+json"})
     *     public Response updateHal(@PathParam("id") ID id, Entity entity) {
     *         if (BookEntity.findById(id) != null) {
     *             entity.id = id;
     *             JpaOperations.getEntityManager().merge(entity);
     *             return Response.status(204).build();
     *         } else {
     *             entity.id = id;
     *             Entity newEntity = JpaOperations.getEntityManager().merge(entity);
     *             HalEntityWrapper wrapper = new HalEntityWrapper(newEntity);
     *             String location = new ResourceLinksProvider().getSelfLink(newEntity);
     *             if (location != null) {
     *                 ResponseBuilder responseBuilder = Response.status(201);
     *                 responseBuilder.entity(wrapper);
     *                 responseBuilder.location(URI.create(location));
     *                 return responseBuilder.build();
     *             } else {
     *                 throw new RuntimeException("Could not extract a new entity URL")
     *             }
     *         }
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodCreator methodCreator = classCreator.getMethodCreator(
                NAME, Response.class.getName(), resourceInfo.getIdClassName(), resourceInfo.getEntityClassName());
        addTransactionalAnnotation(methodCreator);
        addPutAnnotation(methodCreator);
        addPathAnnotation(methodCreator, propertiesAccessor
                .getPath(resourceInfo.getClassInfo(), getStandardMethodMetadata(resourceInfo), "{id}"));
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addConsumesAnnotation(methodCreator, MethodImplementor.APPLICATION_JSON);
        addProducesAnnotation(methodCreator, APPLICATION_HAL_JSON);

        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entity = methodCreator.getMethodParam(1);
        BranchResult entityDoesNotExist = methodCreator
                .ifNull(resourceInfo.getDataAccessImplementor().findById(methodCreator, id));

        FieldAccessImplementor fieldAccessImplementor = new FieldAccessImplementor(index, resourceInfo.getIdFieldPredicate());
        createAndReturn(entityDoesNotExist.trueBranch(), fieldAccessImplementor, resourceInfo, entity, id);
        updateAndReturn(entityDoesNotExist.falseBranch(), fieldAccessImplementor, resourceInfo, entity, id);
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getStandardMethodMetadata(RestDataResourceInfo resourceInfo) {
        return new MethodMetadata(UpdateMethodImplementor.NAME, resourceInfo.getIdClassName(),
                resourceInfo.getEntityClassName());
    }

    private void createAndReturn(BytecodeCreator creator, FieldAccessImplementor fieldAccessImplementor,
            RestDataResourceInfo resourceInfo, ResultHandle entity, ResultHandle id) {
        fieldAccessImplementor.setId(creator, resourceInfo.getEntityClassName(), entity, id);
        ResultHandle newEntity = resourceInfo.getDataAccessImplementor().update(creator, entity);
        ResultHandle response = ResponseImplementor
                .created(creator, wrapHalEntity(creator, newEntity), ResponseImplementor.getEntityUrl(creator, newEntity));
        creator.returnValue(response);
    }

    private void updateAndReturn(BytecodeCreator creator, FieldAccessImplementor fieldAccessImplementor,
            RestDataResourceInfo resourceInfo, ResultHandle entity, ResultHandle id) {
        fieldAccessImplementor.setId(creator, resourceInfo.getEntityClassName(), entity, id);
        resourceInfo.getDataAccessImplementor().update(creator, entity);
        creator.returnValue(ResponseImplementor.noContent(creator));
    }
}
