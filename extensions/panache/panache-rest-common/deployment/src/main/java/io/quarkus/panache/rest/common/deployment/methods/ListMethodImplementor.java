package io.quarkus.panache.rest.common.deployment.methods;

import java.util.List;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.DataAccessImplementor;
import io.quarkus.panache.rest.common.deployment.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceAnnotator;
import io.quarkus.panache.rest.common.deployment.utils.UrlImplementor;

public final class ListMethodImplementor implements MethodImplementor {

    public static final String NAME = "list";

    private final DataAccessImplementor dataAccessImplementor;

    private final String entityClassName;

    public ListMethodImplementor(DataAccessImplementor dataAccessImplementor, String entityClassName) {
        this.dataAccessImplementor = dataAccessImplementor;
        this.entityClassName = entityClassName;
    }

    /**
     * Implements {@link PanacheCrudResource#list()}.
     * Generated code looks more or less like this:
     * 
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("entities")
     *     &#64;Produces({"application/json"})
     *     &#64;LinkResource(
     *         rel = "list",
     *         entityClassName = "com.example.Entity"
     *     )
     *     public List list() {
     *         return Entity.listAll();
     *     }
     * }
     * </pre>
     *
     * @param classCreator
     */
    @Override
    public void implement(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator.getMethodCreator(NAME, List.class);
        ResourceAnnotator.addGet(methodCreator);
        ResourceAnnotator.addPath(methodCreator, UrlImplementor.getCollectionUrl(entityClassName));
        ResourceAnnotator.addProduces(methodCreator, ResourceAnnotator.APPLICATION_JSON);
        ResourceAnnotator.addLinks(methodCreator, entityClassName, "list");

        methodCreator.returnValue(dataAccessImplementor.listAll(methodCreator));
        methodCreator.close();
    }
}
