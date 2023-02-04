package io.quarkus.spring.data.rest.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
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
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(className)
                .interfaces(RestDataResource.class)
                .build();

        classCreator.addAnnotation(ApplicationScoped.class);
        methodsImplementor.implementList(classCreator, resourceType);
        methodsImplementor.implementListPageCount(classCreator, resourceType);
        methodsImplementor.implementGet(classCreator, resourceType);
        methodsImplementor.implementAdd(classCreator, resourceType);
        methodsImplementor.implementUpdate(classCreator, resourceType, entityType);
        methodsImplementor.implementDelete(classCreator, resourceType);

        classCreator.close();
        LOGGER.tracef("Completed generation of '%s'", className);
        return className;
    }
}
