package io.quarkus.panache.rest.common.deployment.methods;

import javax.ws.rs.core.Response;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceInfo;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;
import io.quarkus.panache.rest.common.deployment.utils.FieldAccessImplementor;
import io.quarkus.panache.rest.common.deployment.utils.ResponseImplementor;

public final class UpdateMethodImplementor extends StandardMethodImplementor {

    public static final String NAME = "update";

    private static final String REL = "update";

    /**
     * Implements {@link PanacheCrudResource#update(Object, Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;Transactional
     *     &#64;PUT
     *     &#64;Path("{id}")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "update",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Response update(@PathParam("id") ID id, Entity entity) {
     *         if (BookEntity.findById(id) != null) {
     *             entity.id = id;
     *             JpaOperations.getEntityManager().merge(entity);
     *             return Response.status(204).build();
     *         } else {
     *             entity.id = id;
     *             Entity newEntity = JpaOperations.getEntityManager().merge(entity);
     *             String location = new ResourceLinksProvider().getSelfLink(newEntity);
     *             if (location != null) {
     *                 ResponseBuilder responseBuilder = Response.status(201);
     *                 responseBuilder.entity(entity);
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
    protected void implementInternal(ClassCreator classCreator, IndexView index, OperationPropertiesAccessor propertiesAccessor,
            PanacheCrudResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        MethodCreator methodCreator = classCreator
                .getMethodCreator(methodMetadata.getName(), Response.class.getName(), methodMetadata.getParameterTypes());
        addTransactionalAnnotation(methodCreator);
        addPutAnnotation(methodCreator);
        addPathAnnotation(methodCreator,
                propertiesAccessor.getPath(resourceInfo.getResourceClassInfo(), methodMetadata, "{id}"));
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addConsumesAnnotation(methodCreator, APPLICATION_JSON);
        addProducesAnnotation(methodCreator, APPLICATION_JSON);
        addLinksAnnotation(methodCreator, resourceInfo.getEntityClassName(), REL);

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
    protected MethodMetadata getMethodMetadata(PanacheCrudResourceInfo resourceInfo) {
        return new MethodMetadata(NAME, resourceInfo.getIdClassName(), resourceInfo.getEntityClassName());
    }

    private void createAndReturn(BytecodeCreator creator, FieldAccessImplementor fieldAccessImplementor,
            PanacheCrudResourceInfo resourceInfo, ResultHandle entity, ResultHandle id) {
        fieldAccessImplementor.setId(creator, resourceInfo.getEntityClassName(), entity, id);
        ResultHandle newEntity = resourceInfo.getDataAccessImplementor().update(creator, entity);
        ResultHandle response = ResponseImplementor.created(creator, newEntity);
        creator.returnValue(response);
    }

    private void updateAndReturn(BytecodeCreator creator, FieldAccessImplementor fieldAccessImplementor,
            PanacheCrudResourceInfo resourceInfo, ResultHandle entity, ResultHandle id) {
        fieldAccessImplementor.setId(creator, resourceInfo.getEntityClassName(), entity, id);
        resourceInfo.getDataAccessImplementor().update(creator, entity);
        creator.returnValue(ResponseImplementor.noContent(creator));
    }
}
