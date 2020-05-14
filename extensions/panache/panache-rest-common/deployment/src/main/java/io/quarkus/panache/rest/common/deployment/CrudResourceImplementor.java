package io.quarkus.panache.rest.common.deployment;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Path;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.panache.rest.common.deployment.methods.AddMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.DeleteMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.GetMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.ListMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.MethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.UpdateMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.hal.AddHalMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.hal.GetHalMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.hal.ListHalMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.hal.UpdateHalMethodImplementor;
import io.quarkus.panache.rest.common.deployment.properties.OperationPropertiesAccessor;
import io.quarkus.panache.rest.common.deployment.properties.ResourcePropertiesAccessor;

class CrudResourceImplementor {

    private static final Logger LOGGER = Logger.getLogger(CrudResourceImplementor.class);

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

    private final OperationPropertiesAccessor operationPropertiesAccessor;

    public CrudResourceImplementor(IndexView index) {
        this.index = index;
        this.resourcePropertiesAccessor = new ResourcePropertiesAccessor(index);
        this.operationPropertiesAccessor = new OperationPropertiesAccessor(index);
    }

    void implement(ClassOutput classOutput, PanacheCrudResourceInfo resourceInfo) {
        String resourceInterfaceName = resourceInfo.getResourceClassInfo().toString();
        String implementationClassName = resourceInterfaceName + "Impl_" + HashUtil.sha1(resourceInterfaceName);
        LOGGER.tracef("Starting generation of '%s'", implementationClassName);
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(implementationClassName)
                .interfaces(resourceInterfaceName)
                .build();
        classCreator.addAnnotation(Path.class)
                .addValue("value", resourcePropertiesAccessor.path(resourceInfo.getResourceClassInfo()));
        implementMethods(classCreator, resourceInfo);
        classCreator.close();
        LOGGER.tracef("Completed generation of '%s'", implementationClassName);
    }

    private void implementMethods(ClassCreator classCreator, PanacheCrudResourceInfo resourceInfo) {
        for (MethodImplementor methodImplementor : STANDARD_METHOD_IMPLEMENTORS) {
            methodImplementor.implement(classCreator, index, operationPropertiesAccessor, resourceInfo);
        }
        if (resourcePropertiesAccessor.isHal(resourceInfo.getResourceClassInfo())) {
            for (MethodImplementor methodImplementor : HAL_METHOD_IMPLEMENTORS) {
                methodImplementor.implement(classCreator, index, operationPropertiesAccessor, resourceInfo);
            }
        }
    }
}
