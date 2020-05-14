package io.quarkus.panache.rest.common.deployment.methods;

import java.util.List;

import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.panache.rest.common.PanacheCrudResource;
import io.quarkus.panache.rest.common.deployment.PanacheCrudResourceInfo;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;

public final class ListMethodImplementor extends StandardMethodImplementor {

    public static final String NAME = "list";

    private static final String REL = "list";

    /**
     * Implements {@link PanacheCrudResource#list()}.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;GET
     *     &#64;Path("")
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
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, IndexView index, OperationPropertiesAccessor propertiesAccessor,
            PanacheCrudResourceInfo resourceInfo) {
        MethodMetadata methodMetadata = getMethodMetadata(resourceInfo);
        MethodCreator methodCreator = classCreator.getMethodCreator(methodMetadata.getName(), List.class);
        addGetAnnotation(methodCreator);
        addPathAnnotation(methodCreator, propertiesAccessor.getPath(resourceInfo.getResourceClassInfo(), methodMetadata));
        addProducesAnnotation(methodCreator, APPLICATION_JSON);
        addLinksAnnotation(methodCreator, resourceInfo.getEntityClassName(), REL);

        methodCreator.returnValue(resourceInfo.getDataAccessImplementor().listAll(methodCreator));
        methodCreator.close();
    }

    @Override
    protected MethodMetadata getMethodMetadata(PanacheCrudResourceInfo resourceInfo) {
        return new MethodMetadata(NAME);
    }
}
