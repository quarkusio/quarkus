package io.quarkus.spring.data.rest.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.runtime.util.HashUtil;

public class ResourceImplementor {

    private static final Logger LOGGER = Logger.getLogger(ResourceImplementor.class);

    private final ResourceMethodsImplementor methodsImplementor;

    protected ResourceImplementor(ResourceMethodsImplementor methodsImplementor) {
        this.methodsImplementor = methodsImplementor;
    }

    /**
     * Implements {@link io.quarkus.rest.data.panache.RestDataResource} interfaces defined in a user application.
     * Instances of this class are registered as beans and are later used in the generated JAX-RS controllers.
     */
    public String implement(ClassOutput classOutput, String resourceType, String entityType) {
        String className = resourceType + "ResourceImpl_" + HashUtil.sha1(resourceType);
        LOGGER.tracef("Starting generation of '%s'", className);

        Gizmo.create(classOutput).class_(className, cc -> {
            cc.implements_(RestDataResource.class);
            cc.addAnnotation(ApplicationScoped.class);
            cc.defaultConstructor();
            methodsImplementor.implementIterable(cc, resourceType);
            methodsImplementor.implementList(cc, resourceType);
            methodsImplementor.implementPagedList(cc, resourceType);
            methodsImplementor.implementAddList(cc, resourceType);
            methodsImplementor.implementListById(cc, resourceType);
            methodsImplementor.implementListPageCount(cc, resourceType);
            methodsImplementor.implementGet(cc, resourceType);
            methodsImplementor.implementAdd(cc, resourceType);
            methodsImplementor.implementUpdate(cc, resourceType, entityType);
            methodsImplementor.implementDelete(cc, resourceType);
        });

        LOGGER.tracef("Completed generation of '%s'", className);
        return className;
    }
}
