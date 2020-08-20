package io.quarkus.rest.data.panache.deployment;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.jboss.logging.Logger;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.rest.data.panache.deployment.methods.AddMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.DeleteMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.GetMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.ListMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.MethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.UpdateMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.hal.AddHalMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.hal.GetHalMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.hal.ListHalMethodImplementor;
import io.quarkus.rest.data.panache.deployment.methods.hal.UpdateHalMethodImplementor;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.runtime.util.HashUtil;

/**
 * Implement a JAX-RS controller exposing a specific {@link io.quarkus.rest.data.panache.RestDataResource} implementation.
 */
class JaxRsResourceImplementor {

    private static final Logger LOGGER = Logger.getLogger(JaxRsResourceImplementor.class);

    private final List<MethodImplementor> METHOD_IMPLEMENTORS = Arrays.asList(
            new GetMethodImplementor(),
            new GetHalMethodImplementor(),
            new ListMethodImplementor(),
            new ListHalMethodImplementor(),
            new AddMethodImplementor(),
            new AddHalMethodImplementor(),
            new UpdateMethodImplementor(),
            new UpdateHalMethodImplementor(),
            new DeleteMethodImplementor());

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
    void implement(ClassOutput classOutput, ResourceMetadata resourceMetadata, ResourceProperties resourceProperties) {
        String controllerClassName = resourceMetadata.getResourceInterface() + "JaxRs_" +
                HashUtil.sha1(resourceMetadata.getResourceInterface());
        LOGGER.tracef("Starting generation of '%s'", controllerClassName);
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput).className(controllerClassName)
                .build();

        implementClassAnnotations(classCreator, resourceProperties);
        FieldDescriptor resourceField = implementResourceField(classCreator, resourceMetadata);
        implementMethods(classCreator, resourceMetadata, resourceProperties, resourceField);

        classCreator.close();
        LOGGER.tracef("Completed generation of '%s'", controllerClassName);
    }

    private void implementClassAnnotations(ClassCreator classCreator, ResourceProperties resourceProperties) {
        classCreator.addAnnotation(Path.class).addValue("value", resourceProperties.getPath());
    }

    private FieldDescriptor implementResourceField(ClassCreator classCreator, ResourceMetadata resourceMetadata) {
        FieldCreator resourceFieldCreator = classCreator.getFieldCreator("resource", resourceMetadata.getResourceClass());
        resourceFieldCreator.addAnnotation(Inject.class);
        return resourceFieldCreator.getFieldDescriptor();
    }

    private void implementMethods(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        for (MethodImplementor methodImplementor : METHOD_IMPLEMENTORS) {
            methodImplementor.implement(classCreator, resourceMetadata, resourceProperties, resourceField);
        }
    }
}
