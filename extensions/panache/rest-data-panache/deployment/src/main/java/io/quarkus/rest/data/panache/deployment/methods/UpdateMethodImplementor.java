package io.quarkus.rest.data.panache.deployment.methods;

import javax.ws.rs.core.Response;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.DataAccessImplementor;
import io.quarkus.rest.data.panache.deployment.RestDataEntityInfo;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;

public final class UpdateMethodImplementor extends StandardMethodImplementor {

    public static final String NAME = "update";

    private static final String REL = "update";

    /**
     * Implements {@link RestDataResource#update(Object, Object)}.
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
    protected void implementInternal(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        MethodCreator methodCreator = classCreator
                .getMethodCreator(methodMetadata.getName(), Response.class.getName(), methodMetadata.getParameterTypes());
        addTransactionalAnnotation(methodCreator);
        addPutAnnotation(methodCreator);
        addPathAnnotation(methodCreator, propertiesAccessor.getPath(resourceInfo.getType(), methodMetadata, "{id}"));
        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addConsumesAnnotation(methodCreator, APPLICATION_JSON);
        addProducesAnnotation(methodCreator, APPLICATION_JSON);
        addLinksAnnotation(methodCreator, resourceInfo.getEntityInfo().getType(), REL);

        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entity = methodCreator.getMethodParam(1);
        setId(methodCreator, resourceInfo.getEntityInfo(), entity, id);

        DataAccessImplementor dataAccessImplementor = resourceInfo.getDataAccessImplementor();
        BranchResult entityDoesNotExist = methodCreator.ifNull(dataAccessImplementor.findById(methodCreator, id));
        createAndReturn(entityDoesNotExist.trueBranch(), dataAccessImplementor, entity);
        updateAndReturn(entityDoesNotExist.falseBranch(), dataAccessImplementor, entity);
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getMethodMetadata(RestDataResourceInfo resourceInfo) {
        return new MethodMetadata(NAME, resourceInfo.getEntityInfo().getIdType(), resourceInfo.getEntityInfo().getType());
    }

    private void createAndReturn(BytecodeCreator creator, DataAccessImplementor dataAccessImplementor, ResultHandle entity) {
        ResultHandle newEntity = dataAccessImplementor.update(creator, entity);
        ResultHandle response = ResponseImplementor.created(creator, newEntity);
        creator.returnValue(response);
    }

    private void updateAndReturn(BytecodeCreator creator, DataAccessImplementor dataAccessImplementor, ResultHandle entity) {
        dataAccessImplementor.update(creator, entity);
        creator.returnValue(ResponseImplementor.noContent(creator));
    }

    private void setId(BytecodeCreator creator, RestDataEntityInfo entityInfo, ResultHandle entity, ResultHandle id) {
        if (entityInfo.getIdSetter().isPresent()) {
            creator.invokeVirtualMethod(entityInfo.getIdSetter().get(), entity, id);
        } else {
            creator.writeInstanceField(entityInfo.getIdField(), entity, id);
        }
    }
}
