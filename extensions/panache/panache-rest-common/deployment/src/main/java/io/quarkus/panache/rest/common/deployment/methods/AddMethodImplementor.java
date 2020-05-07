package io.quarkus.panache.rest.common.deployment.methods;

import javax.ws.rs.core.Response;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.DataAccessImplementor;
import io.quarkus.panache.rest.common.deployment.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceAnnotator;
import io.quarkus.panache.rest.common.deployment.utils.ResponseImplementor;
import io.quarkus.panache.rest.common.deployment.utils.UrlImplementor;

public final class AddMethodImplementor implements MethodImplementor {

    public static final String NAME = "add";

    private final DataAccessImplementor dataAccessImplementor;

    private final UrlImplementor urlProvider;

    private final String entityClassName;

    public AddMethodImplementor(DataAccessImplementor dataAccessImplementor, UrlImplementor urlProvider,
            String entityClassName) {
        this.dataAccessImplementor = dataAccessImplementor;
        this.urlProvider = urlProvider;
        this.entityClassName = entityClassName;
    }

    /**
     * Implements {@link PanacheCrudResource#add(Object)}.
     * Generated code looks more or less like this:
     * 
     * <pre>
     * {@code
     *     &#64;Transactional
     *     &#64;POST
     *     &#64;Path("entities")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "add",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public Response add(Entity entity) {
     *         entity.persist();
     *         String id = String.valueOf(entity.id);
     *         CharSequence[] pathElements = new CharSequence[]{"entities", id};
     *         URI uri = URI.create(String.join("/", pathElements));
     *         ResponseBuilder responseBuilder = Response.status(201);
     *         responseBuilder.entity(entity);
     *         responseBuilder.location(uri);
     *         return responseBuilder.build();
     *     }
     * }
     * </pre>
     *
     * @param classCreator
     */
    @Override
    public void implement(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator.getMethodCreator(NAME, Response.class, entityClassName);
        ResourceAnnotator.addTransactional(methodCreator);
        ResourceAnnotator.addPost(methodCreator);
        ResourceAnnotator.addPath(methodCreator, UrlImplementor.getCollectionUrl(entityClassName));
        ResourceAnnotator.addConsumes(methodCreator, ResourceAnnotator.APPLICATION_JSON);
        ResourceAnnotator.addProduces(methodCreator, ResourceAnnotator.APPLICATION_JSON);
        ResourceAnnotator.addLinks(methodCreator, entityClassName, "add");

        ResultHandle entity = methodCreator.getMethodParam(0);
        dataAccessImplementor.persist(methodCreator, entity);
        ResultHandle url = urlProvider.getEntityUrl(methodCreator, entity, entityClassName);

        methodCreator.returnValue(ResponseImplementor.created(methodCreator, entity, url));
        methodCreator.close();
    }
}
