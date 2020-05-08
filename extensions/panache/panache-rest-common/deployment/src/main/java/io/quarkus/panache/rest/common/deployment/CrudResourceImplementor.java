package io.quarkus.panache.rest.common.deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.panache.rest.common.deployment.methods.AddHalMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.AddMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.DeleteMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.GetHalMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.GetMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.ListHalMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.ListMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.NotExposedMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.UpdateHalMethodImplementor;
import io.quarkus.panache.rest.common.deployment.methods.UpdateMethodImplementor;
import io.quarkus.panache.rest.common.deployment.utils.FieldAccessImplementor;
import io.quarkus.panache.rest.common.deployment.utils.PanacheRestResourceAccessor;
import io.quarkus.panache.rest.common.deployment.utils.ResourceAnnotator;
import io.quarkus.panache.rest.common.deployment.utils.UrlImplementor;

class CrudResourceImplementor {

    private static final Logger LOGGER = Logger.getLogger(CrudResourceImplementor.class);

    private final IndexView index;

    private final PanacheRestResourceAccessor panacheRestResourceAccessor;

    private FieldDescriptor uriInfoField;

    public CrudResourceImplementor(IndexView index) {
        this.index = index;
        this.panacheRestResourceAccessor = new PanacheRestResourceAccessor(index);
    }

    void implement(ClassOutput classOutput, PanacheCrudResourceBuildItem resource) {
        String resourceInterfaceName = resource.getResourceClassInfo().toString();
        String implementationClassName = resourceInterfaceName + "Impl_" + HashUtil.sha1(resourceInterfaceName);
        LOGGER.tracef("Starting generation of '%s'", implementationClassName);

        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(implementationClassName)
                .interfaces(resourceInterfaceName)
                .build();
        ResourceAnnotator.addPath(classCreator, "/");
        uriInfoField = implementUriInfoField(classCreator);

        for (MethodImplementor methodImplementor : getMethodImplementors(resource)) {
            methodImplementor.implement(classCreator);
        }

        classCreator.close();
        LOGGER.tracef("Completed generation of '%s'", implementationClassName);
    }

    private FieldDescriptor implementUriInfoField(ClassCreator classCreator) {
        FieldCreator fieldCreator = classCreator.getFieldCreator("uriInfo", UriInfo.class);
        fieldCreator.addAnnotation(Context.class);
        return fieldCreator.getFieldDescriptor();
    }

    private List<MethodImplementor> getMethodImplementors(PanacheCrudResourceBuildItem resource) {
        FieldAccessImplementor fieldAccessImplementor = new FieldAccessImplementor(index, resource.getIdFieldPredicate());
        UrlImplementor urlImplementor = new UrlImplementor(fieldAccessImplementor);
        List<MethodImplementor> methodImplementors = new LinkedList<>();
        ClassInfo resourceClassInfo = resource.getResourceClassInfo();
        DataAccessImplementor dataAccessImplementor = resource.getDataAccessImplementor();
        String idClassName = resource.getIdClassName();
        String entityClassName = resource.getEntityClassName();

        // Basic
        methodImplementors.add(getBasicMethodImplementor(
                () -> new AddMethodImplementor(dataAccessImplementor, urlImplementor, entityClassName),
                resourceClassInfo, AddMethodImplementor.NAME, entityClassName));

        methodImplementors.add(getBasicMethodImplementor(
                () -> new DeleteMethodImplementor(dataAccessImplementor, idClassName, entityClassName),
                resourceClassInfo, DeleteMethodImplementor.NAME, idClassName));

        methodImplementors.add(getBasicMethodImplementor(
                () -> new GetMethodImplementor(dataAccessImplementor, idClassName, entityClassName),
                resourceClassInfo, GetMethodImplementor.NAME, idClassName));

        methodImplementors.add(getBasicMethodImplementor(
                () -> new ListMethodImplementor(dataAccessImplementor, entityClassName, uriInfoField),
                resourceClassInfo, ListMethodImplementor.NAME));

        methodImplementors.add(getBasicMethodImplementor(
                () -> new UpdateMethodImplementor(dataAccessImplementor, fieldAccessImplementor, urlImplementor, idClassName,
                        entityClassName),
                resourceClassInfo, UpdateMethodImplementor.NAME, idClassName, entityClassName));

        // Hal
        getHalMethodImplementor(() -> new AddHalMethodImplementor(dataAccessImplementor, urlImplementor, entityClassName),
                resourceClassInfo, AddMethodImplementor.NAME, entityClassName).ifPresent(methodImplementors::add);

        getHalMethodImplementor(() -> new GetHalMethodImplementor(dataAccessImplementor, idClassName, entityClassName),
                resourceClassInfo, GetMethodImplementor.NAME, idClassName).ifPresent(methodImplementors::add);

        getHalMethodImplementor(() -> new ListHalMethodImplementor(dataAccessImplementor, entityClassName, uriInfoField),
                resourceClassInfo, ListMethodImplementor.NAME).ifPresent(methodImplementors::add);

        getHalMethodImplementor(
                () -> new UpdateHalMethodImplementor(dataAccessImplementor, fieldAccessImplementor, urlImplementor, idClassName,
                        entityClassName),
                resourceClassInfo, UpdateMethodImplementor.NAME, idClassName, entityClassName)
                        .ifPresent(methodImplementors::add);

        return methodImplementors;
    }

    private MethodImplementor getBasicMethodImplementor(Supplier<MethodImplementor> supplier, ClassInfo resourceClassInfo,
            String name, String... parameterTypes) {
        if (panacheRestResourceAccessor.isExposed(resourceClassInfo, name, parameterTypes)) {
            return supplier.get();
        }
        return new NotExposedMethodImplementor(name, parameterTypes);
    }

    private Optional<MethodImplementor> getHalMethodImplementor(Supplier<MethodImplementor> supplier,
            ClassInfo resourceClassInfo, String name, String... parameterTypes) {
        if (panacheRestResourceAccessor.isExposed(resourceClassInfo, name, parameterTypes)
                && panacheRestResourceAccessor.isHal(resourceClassInfo, name, parameterTypes)) {
            return Optional.of(supplier.get());
        }
        return Optional.empty();
    }
}
