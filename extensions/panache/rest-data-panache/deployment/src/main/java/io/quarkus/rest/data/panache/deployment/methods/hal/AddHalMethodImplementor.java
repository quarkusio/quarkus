package io.quarkus.rest.data.panache.deployment.methods.hal;

import javax.ws.rs.core.Response;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;
import io.quarkus.rest.data.panache.deployment.methods.AddMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodMetadata;
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;

public final class AddHalMethodImplementor extends HalMethodImplementor {

    private static final String NAME = "addHal";

    /**
     * Implements HAL version of {@link RestDataResource#add(Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;Transactional
     *     &#64;POST
     *     &#64;Path("")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/hal+json"})
     *     public Response addHal(Entity entity) {
     *         entity.persist();
     *         HalEntityWrapper wrapper = new HalEntityWrapper(entity);
     *         String location = new ResourceLinksProvider().getSelfLink(entity);
     *         if (location != null) {
     *             ResponseBuilder responseBuilder = Response.status(201);
     *             responseBuilder.entity(wrapper);
     *             responseBuilder.location(URI.create(location));
     *             return responseBuilder.build();
     *         } else {
     *             throw new RuntimeException("Could not extract a new entity URL")
     *         }
     *     }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, IndexView index, MethodPropertiesAccessor propertiesAccessor,
            RestDataResourceInfo resourceInfo) {
        MethodCreator methodCreator = classCreator
                .getMethodCreator(NAME, Response.class.getName(), resourceInfo.getEntityInfo().getType());
        addTransactionalAnnotation(methodCreator);
        addPostAnnotation(methodCreator);
        addPathAnnotation(methodCreator,
                propertiesAccessor.getPath(resourceInfo.getType(), getMethodMetadata(resourceInfo)));
        addConsumesAnnotation(methodCreator, APPLICATION_JSON);
        addProducesAnnotation(methodCreator, APPLICATION_HAL_JSON);

        ResultHandle entity = methodCreator.getMethodParam(0);
        resourceInfo.getDataAccessImplementor().persist(methodCreator, entity);

        methodCreator.returnValue(ResponseImplementor.created(methodCreator, wrapHalEntity(methodCreator, entity),
                ResponseImplementor.getEntityUrl(methodCreator, entity)));
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getMethodMetadata(RestDataResourceInfo resourceInfo) {
        return new MethodMetadata(AddMethodImplementor.NAME, resourceInfo.getEntityInfo().getType());
    }
}
