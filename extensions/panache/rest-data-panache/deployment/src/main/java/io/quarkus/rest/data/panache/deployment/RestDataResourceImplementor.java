package io.quarkus.rest.data.panache.deployment;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Path;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
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
import io.quarkus.rest.data.panache.deployment.properties.MethodPropertiesAccessor;
import io.quarkus.rest.data.panache.deployment.properties.ResourcePropertiesAccessor;

class RestDataResourceImplementor {

    private static final Logger LOGGER = Logger.getLogger(RestDataResourceImplementor.class);

    private static final List<MethodImplementor> STANDARD_METHOD_IMPLEMENTORS = Arrays.asList(
            new GetMethodImplementor(),
            new ListMethodImplementor(),
            new AddMethodImplementor(),
            new UpdateMethodImplementor(),
            new DeleteMethodImplementor());

    private static final List<MethodImplementor> HAL_METHOD_IMPLEMENTORS = Arrays.asList(
            new GetHalMethodImplementor(),
            new ListHalMethodImplementor(),
            new AddHalMethodImplementor(),
            new UpdateHalMethodImplementor());

    private final IndexView index;

    private final ResourcePropertiesAccessor resourcePropertiesAccessor;

    private final MethodPropertiesAccessor methodPropertiesAccessor;

    public RestDataResourceImplementor(IndexView index) {
        this.index = index;
        this.resourcePropertiesAccessor = new ResourcePropertiesAccessor(index);
        this.methodPropertiesAccessor = new MethodPropertiesAccessor(index);
    }

    void implement(ClassOutput classOutput, RestDataResourceInfo resourceInfo) {
        String resourceInterfaceName = resourceInfo.getClassInfo().toString();
        String implementationClassName = resourceInterfaceName + "Impl_" + HashUtil.sha1(resourceInterfaceName);
        LOGGER.tracef("Starting generation of '%s'", implementationClassName);
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(implementationClassName)
                .interfaces(resourceInterfaceName)
                .build();
        classCreator.addAnnotation(Path.class)
                .addValue("value", resourcePropertiesAccessor.path(resourceInfo.getClassInfo()));
        implementMethods(classCreator, resourceInfo);
        classCreator.close();
        LOGGER.tracef("Completed generation of '%s'", implementationClassName);
    }

    private void implementMethods(ClassCreator classCreator, RestDataResourceInfo resourceInfo) {
        for (MethodImplementor methodImplementor : STANDARD_METHOD_IMPLEMENTORS) {
            methodImplementor.implement(classCreator, index, methodPropertiesAccessor, resourceInfo);
        }
        if (resourcePropertiesAccessor.isHal(resourceInfo.getClassInfo())) {
            for (MethodImplementor methodImplementor : HAL_METHOD_IMPLEMENTORS) {
                methodImplementor.implement(classCreator, index, methodPropertiesAccessor, resourceInfo);
            }
        }
    }
}
