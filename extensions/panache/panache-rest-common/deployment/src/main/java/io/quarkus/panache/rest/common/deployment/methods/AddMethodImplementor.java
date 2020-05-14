package io.quarkus.panache.rest.common.deployment.methods;

import javax.ws.rs.core.Response;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceInfo;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;
import io.quarkus.panache.rest.common.deployment.utils.ResponseImplementor;

public final class AddMethodImplementor extends StandardMethodImplementor {

    public static final String NAME = "add";

    private static final String REL = "add";

    /**
     * Implements {@link PanacheCrudResource#add(Object)}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;Transactional
     *     &#64;POST
     *     &#64;Path("")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "add",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Response add(Entity entity) {
     *         entity.persist();
     *         String location = new ResourceLinksProvider().getSelfLink(entity);
     *         if (location != null) {
     *             ResponseBuilder responseBuilder = Response.status(201);
     *             responseBuilder.entity(entity);
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
    protected void implementInternal(ClassCreator classCreator, IndexView index, OperationPropertiesAccessor propertiesAccessor,
            PanacheCrudResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        MethodCreator methodCreator = classCreator
                .getMethodCreator(methodMetadata.getName(), Response.class.getName(), methodMetadata.getParameterTypes());
        addTransactionalAnnotation(methodCreator);
        addPostAnnotation(methodCreator);
        addPathAnnotation(methodCreator, propertiesAccessor.getPath(resourceInfo.getResourceClassInfo(), methodMetadata));
        addConsumesAnnotation(methodCreator, APPLICATION_JSON);
        addProducesAnnotation(methodCreator, APPLICATION_JSON);
        addLinksAnnotation(methodCreator, resourceInfo.getEntityClassName(), REL);

        ResultHandle entity = methodCreator.getMethodParam(0);
        resourceInfo.getDataAccessImplementor().persist(methodCreator, entity);

        methodCreator.returnValue(ResponseImplementor.created(methodCreator, entity));
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getMethodMetadata(PanacheCrudResourceInfo resourceInfo) {
        return new MethodMetadata(NAME, resourceInfo.getEntityClassName());
    }
}
