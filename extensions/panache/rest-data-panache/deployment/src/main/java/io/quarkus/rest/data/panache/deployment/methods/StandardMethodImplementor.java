package io.quarkus.rest.data.panache.deployment.methods;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.jboss.logging.Logger;

import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.AnnotationCreator;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.rest.data.panache.RestDataPanacheException;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.runtime.sort.SortQueryParamValidator;
import io.quarkus.resteasy.reactive.links.RestLink;

/**
 * A standard JAX-RS method implementor.
 */
public abstract class StandardMethodImplementor implements MethodImplementor {

    private static final Logger LOGGER = Logger.getLogger(StandardMethodImplementor.class);

    /**
     * Implement exposed JAX-RS method.
     */
    @Override
    public void implement(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        if (resourceProperties.isExposed(getResourceMethodName())) {
            implementInternal(classCreator, resourceMetadata, resourceProperties, resourceField);
        }
    }

    /**
     * Implement the actual JAX-RS method logic.
     */
    protected abstract void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField);

    /**
     * Get a name of a method which this controller uses to access data.
     */
    protected abstract String getResourceMethodName();

    protected TryBlock implementTryBlock(BytecodeCreator bytecodeCreator, String message) {
        TryBlock tryBlock = bytecodeCreator.tryBlock();
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        catchBlock.throwException(RestDataPanacheException.class, message, catchBlock.getCaughtException());
        catchBlock.close();
        return tryBlock;
    }

    protected void addGetAnnotation(AnnotatedElement element) {
        element.addAnnotation(GET.class);
    }

    protected void addPostAnnotation(AnnotatedElement element) {
        element.addAnnotation(POST.class);
    }

    protected void addPutAnnotation(AnnotatedElement element) {
        element.addAnnotation(PUT.class);
    }

    protected void addDeleteAnnotation(AnnotatedElement element) {
        element.addAnnotation(DELETE.class);
    }

    protected void addLinksAnnotation(AnnotatedElement element, String entityClassName, String rel) {
        AnnotationCreator linkResource = element.addAnnotation(RestLink.class);
        Class<?> entityClass;
        try {
            entityClass = Thread.currentThread().getContextClassLoader().loadClass(entityClassName);
            linkResource.addValue("entityType", entityClass);
            linkResource.addValue("rel", rel);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Unable to create links for entity: '" + entityClassName + "'", e);
        }
    }

    protected void addPathAnnotation(AnnotatedElement element, String value) {
        element.addAnnotation(Path.class).addValue("value", value);
    }

    protected void addPathParamAnnotation(AnnotatedElement element, String value) {
        element.addAnnotation(PathParam.class).addValue("value", value);
    }

    protected void addQueryParamAnnotation(AnnotatedElement element, String value) {
        element.addAnnotation(QueryParam.class).addValue("value", value);
    }

    protected void addDefaultValueAnnotation(AnnotatedElement element, String value) {
        element.addAnnotation(DefaultValue.class).addValue("value", value);
    }

    protected void addProducesAnnotation(AnnotatedElement element, String... mediaTypes) {
        element.addAnnotation(Produces.class).addValue("value", mediaTypes);
    }

    protected void addConsumesAnnotation(AnnotatedElement element, String... mediaTypes) {
        element.addAnnotation(Consumes.class).addValue("value", mediaTypes);
    }

    protected void addContextAnnotation(AnnotatedElement element) {
        element.addAnnotation(Context.class);
    }

    protected void addSortQueryParamValidatorAnnotation(AnnotatedElement element) {
        element.addAnnotation(SortQueryParamValidator.class);
    }

    protected String appendToPath(String path, String suffix) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.lastIndexOf("/"));
        }
        if (suffix.startsWith("/")) {
            suffix = suffix.substring(1);
        }
        return String.join("/", path, suffix);
    }
}
