package io.quarkus.rest.data.panache.deployment.methods;

import java.util.Collection;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
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
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;
import io.quarkus.rest.data.panache.runtime.sort.SortQueryParamValidator;

/**
 * A standard JAX-RS method implementor.
 */
public abstract class StandardMethodImplementor implements MethodImplementor {
    private static final String OPENAPI_PACKAGE = "org.eclipse.microprofile.openapi.annotations";
    private static final String OPENAPI_RESPONSE_ANNOTATION = OPENAPI_PACKAGE + ".responses.APIResponse";
    private static final String OPENAPI_CONTENT_ANNOTATION = OPENAPI_PACKAGE + ".media.Content";
    private static final String OPENAPI_SCHEMA_ANNOTATION = OPENAPI_PACKAGE + ".media.Schema";
    private static final String SCHEMA_TYPE_ARRAY = "ARRAY";
    private static final String ROLES_ALLOWED_ANNOTATION = "jakarta.annotation.security.RolesAllowed";
    private static final Logger LOGGER = Logger.getLogger(StandardMethodImplementor.class);

    protected final ResponseImplementor responseImplementor;
    private final Capabilities capabilities;

    protected StandardMethodImplementor(Capabilities capabilities) {
        this.capabilities = capabilities;
        this.responseImplementor = new ResponseImplementor(capabilities);
    }

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
        if (isResteasyClassic()) {
            AnnotationCreator linkResource = element.addAnnotation("org.jboss.resteasy.links.LinkResource");
            linkResource.addValue("entityClassName", entityClassName);
            linkResource.addValue("rel", rel);
        } else {
            AnnotationCreator linkResource = element.addAnnotation("io.quarkus.resteasy.reactive.links.RestLink");
            Class<?> entityClass;
            try {
                entityClass = Thread.currentThread().getContextClassLoader().loadClass(entityClassName);
                linkResource.addValue("entityType", entityClass);
                linkResource.addValue("rel", rel);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Unable to create links for entity: '" + entityClassName + "'", e);
            }
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

    protected void addProducesJsonAnnotation(AnnotatedElement element, ResourceProperties properties) {
        if (properties.isHal()) {
            addProducesAnnotation(element, APPLICATION_JSON, APPLICATION_HAL_JSON);
        } else {
            addProducesAnnotation(element, APPLICATION_JSON);
        }
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

    protected void addMethodAnnotations(AnnotatedElement element, Collection<AnnotationInstance> methodAnnotations) {
        if (methodAnnotations != null) {
            for (AnnotationInstance methodAnnotation : methodAnnotations) {
                element.addAnnotation(methodAnnotation);
            }
        }
    }

    protected void addSecurityAnnotations(AnnotatedElement element, ResourceProperties resourceProperties) {
        String[] rolesAllowed = resourceProperties.getRolesAllowed(getResourceMethodName());
        if (rolesAllowed.length > 0 && hasSecurityCapability()) {
            element.addAnnotation(ROLES_ALLOWED_ANNOTATION).add("value", rolesAllowed);
        }
    }

    protected void addOpenApiResponseAnnotation(AnnotatedElement element, Response.Status status) {
        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI)) {
            element.addAnnotation(OPENAPI_RESPONSE_ANNOTATION)
                    .add("responseCode", String.valueOf(status.getStatusCode()));
        }
    }

    protected void addOpenApiResponseAnnotation(AnnotatedElement element, Response.Status status, String entityType) {
        addOpenApiResponseAnnotation(element, status, entityType, false);
    }

    protected void addOpenApiResponseAnnotation(AnnotatedElement element, Response.Status status, String entityType,
            boolean isList) {
        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI)) {
            addOpenApiResponseAnnotation(element, status, toClass(entityType), isList);
        }
    }

    protected void addOpenApiResponseAnnotation(AnnotatedElement element, Response.Status status, Class<?> clazz,
            boolean isList) {
        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI)) {
            AnnotationCreator schemaAnnotation = AnnotationCreator.of(OPENAPI_SCHEMA_ANNOTATION)
                    .add("implementation", clazz);

            if (isList) {
                schemaAnnotation.add("type", SCHEMA_TYPE_ARRAY);
            }

            element.addAnnotation(OPENAPI_RESPONSE_ANNOTATION)
                    .add("responseCode", String.valueOf(status.getStatusCode()))
                    .add("content", new Object[] { AnnotationCreator.of(OPENAPI_CONTENT_ANNOTATION)
                            .add("mediaType", APPLICATION_JSON)
                            .add("schema", schemaAnnotation)
                    });
        }
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

    protected boolean hasSecurityCapability() {
        return capabilities.isPresent(Capability.SECURITY);
    }

    protected boolean hasValidatorCapability() {
        return capabilities.isPresent(Capability.HIBERNATE_VALIDATOR);
    }

    protected boolean isResteasyClassic() {
        return capabilities.isPresent(Capability.RESTEASY);
    }

    protected boolean isNotReactivePanache() {
        return !capabilities.isPresent(Capability.HIBERNATE_REACTIVE);
    }

    private static Class<?> toClass(String className) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The class (" + className + ") cannot be found during deployment.", e);
        }
    }
}
