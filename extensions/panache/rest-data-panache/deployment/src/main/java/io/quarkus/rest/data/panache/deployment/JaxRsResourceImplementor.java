package io.quarkus.rest.data.panache.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

import org.apache.commons.lang3.StringUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.rest.data.panache.deployment.methods.AddMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.CountMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.DeleteMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.GetMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.ListMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.UpdateMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.hal.ListHalMethodImplementor;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.runtime.util.HashUtil;

/**
 * Implement a JAX-RS controller exposing a specific {@link io.quarkus.rest.data.panache.RestDataResource} implementation.
 */
class JaxRsResourceImplementor {

    private static final Logger LOGGER = Logger.getLogger(JaxRsResourceImplementor.class);
    private static final String OPENAPI_TAG_ANNOTATION = "org.eclipse.microprofile.openapi.annotations.tags.Tag";

    private final List<MethodImplementor> methodImplementors;

    JaxRsResourceImplementor(Capabilities capabilities) {
        this.methodImplementors = Arrays.asList(new GetMethodImplementor(capabilities),
                new ListMethodImplementor(capabilities),
                new CountMethodImplementor(capabilities),
                new AddMethodImplementor(capabilities),
                new UpdateMethodImplementor(capabilities),
                new DeleteMethodImplementor(capabilities),
                // The list hal endpoint needs to be added for both resteasy classic and resteasy reactive
                // because the pagination links are programmatically added.
                new ListHalMethodImplementor(capabilities));
    }

    /**
     * Implement a JAX-RS resource with a following structure.
     *
     * <pre>
     * {@code
     *      &#64;Path("/my-entities')
     *      public class MyEntitiesResourceController_1234 {
     *          &#64;Inject
     *          MyEntitiesResource resource;
     *
     *          // JAX-RS method implementations. See method implementors for details.
     *      }
     * }
     * </pre>
     */
    void implement(ClassOutput classOutput, ResourceMetadata resourceMetadata, ResourceProperties resourceProperties,
            Capabilities capabilities) {
        String controllerClassName = resourceMetadata.getResourceInterface() + "JaxRs_" +
                HashUtil.sha1(resourceMetadata.getResourceInterface());
        LOGGER.tracef("Starting generation of '%s'", controllerClassName);
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput).className(controllerClassName)
                .build();

        implementClassAnnotations(classCreator, resourceMetadata, resourceProperties, capabilities);
        FieldDescriptor resourceField = implementResourceField(classCreator, resourceMetadata);
        implementMethods(classCreator, resourceMetadata, resourceProperties, resourceField);

        classCreator.close();
        LOGGER.tracef("Completed generation of '%s'", controllerClassName);
    }

    private void implementClassAnnotations(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, Capabilities capabilities) {
        classCreator.addAnnotation(Path.class).addValue("value", resourceProperties.getPath());
        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI)) {
            String className = StringUtils.substringAfterLast(resourceMetadata.getResourceInterface(), ".");
            classCreator.addAnnotation(OPENAPI_TAG_ANNOTATION).add("name", className);
        }
        if (resourceProperties.getClassAnnotations() != null) {
            for (AnnotationInstance classAnnotation : resourceProperties.getClassAnnotations()) {
                classCreator.addAnnotation(classAnnotation);
            }
        }
    }

    private FieldDescriptor implementResourceField(ClassCreator classCreator, ResourceMetadata resourceMetadata) {
        FieldCreator resourceFieldCreator = classCreator.getFieldCreator("resource", resourceMetadata.getResourceClass());
        resourceFieldCreator.setModifiers(resourceFieldCreator.getModifiers() & ~Modifier.PRIVATE)
                .addAnnotation(Inject.class);
        return resourceFieldCreator.getFieldDescriptor();
    }

    private void implementMethods(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        for (MethodImplementor methodImplementor : methodImplementors) {
            methodImplementor.implement(classCreator, resourceMetadata, resourceProperties, resourceField);
        }
    }
}
