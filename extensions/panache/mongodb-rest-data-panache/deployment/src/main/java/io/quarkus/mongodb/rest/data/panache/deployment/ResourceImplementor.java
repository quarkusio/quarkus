package io.quarkus.mongodb.rest.data.panache.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.jandex.FieldInfo;
import org.jboss.logging.Logger;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.deployment.Constants;
import io.quarkus.runtime.util.HashUtil;

/**
 * {@link io.quarkus.rest.data.panache.RestDataResource} implementor that generates data access logic depending on which
 * sub-interfaces are used in the application.
 * The method implementation differs depending on a data access strategy (active record or repository).
 */
class ResourceImplementor {

    private static final Logger LOGGER = Logger.getLogger(ResourceImplementor.class);

    private final EntityClassHelper entityClassHelper;

    ResourceImplementor(EntityClassHelper entityClassHelper) {
        this.entityClassHelper = entityClassHelper;
    }

    String implement(ClassOutput classOutput, DataAccessImplementor dataAccessImplementor, String resourceType,
            String entityType) {
        String className = resourceType + "Impl_" + HashUtil.sha1(resourceType);
        LOGGER.tracef("Starting generation of '%s'", className);
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(className)
                .interfaces(resourceType)
                .build();

        classCreator.addAnnotation(ApplicationScoped.class);
        implementList(classCreator, dataAccessImplementor);
        implementListPageCount(classCreator, dataAccessImplementor);
        implementGet(classCreator, dataAccessImplementor);
        implementAdd(classCreator, dataAccessImplementor);
        implementUpdate(classCreator, dataAccessImplementor, entityType);
        implementDelete(classCreator, dataAccessImplementor);

        classCreator.close();
        LOGGER.tracef("Completed generation of '%s'", className);
        return className;
    }

    private void implementList(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("list", List.class, Page.class, Sort.class);
        ResultHandle page = methodCreator.getMethodParam(0);
        ResultHandle sort = methodCreator.getMethodParam(1);
        ResultHandle columns = methodCreator.invokeVirtualMethod(ofMethod(Sort.class, "getColumns", List.class), sort);
        ResultHandle isEmptySort = methodCreator.invokeInterfaceMethod(ofMethod(List.class, "isEmpty", boolean.class), columns);

        BranchResult isEmptySortBranch = methodCreator.ifTrue(isEmptySort);
        isEmptySortBranch.trueBranch().returnValue(dataAccessImplementor.findAll(isEmptySortBranch.trueBranch(), page));
        isEmptySortBranch.falseBranch().returnValue(dataAccessImplementor.findAll(isEmptySortBranch.falseBranch(), page, sort));

        methodCreator.close();
    }

    private void implementListPageCount(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator(Constants.PAGE_COUNT_METHOD_PREFIX + "list", int.class,
                Page.class);
        ResultHandle page = methodCreator.getMethodParam(0);
        methodCreator.returnValue(dataAccessImplementor.pageCount(methodCreator, page));
        methodCreator.close();
    }

    private void implementGet(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("get", Object.class, Object.class);
        ResultHandle id = methodCreator.getMethodParam(0);
        methodCreator.returnValue(dataAccessImplementor.findById(methodCreator, id));
        methodCreator.close();
    }

    private void implementAdd(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("add", Object.class, Object.class);
        ResultHandle entity = methodCreator.getMethodParam(0);
        methodCreator.returnValue(dataAccessImplementor.persist(methodCreator, entity));
        methodCreator.close();
    }

    private void implementUpdate(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor,
            String entityType) {
        MethodCreator methodCreator = classCreator.getMethodCreator("update", Object.class, Object.class, Object.class);
        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entity = methodCreator.getMethodParam(1);
        setId(methodCreator, entityType, entity, id);
        methodCreator.returnValue(dataAccessImplementor.persistOrUpdate(methodCreator, entity));
        methodCreator.close();
    }

    private void implementDelete(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("delete", boolean.class, Object.class);
        ResultHandle id = methodCreator.getMethodParam(0);
        methodCreator.returnValue(dataAccessImplementor.deleteById(methodCreator, id));
        methodCreator.close();
    }

    private void setId(BytecodeCreator creator, String entityType, ResultHandle entity, ResultHandle id) {
        FieldInfo idField = entityClassHelper.getIdField(entityType);
        MethodDescriptor idSetter = entityClassHelper.getSetter(entityType, idField);
        if (idSetter == null) {
            creator.writeInstanceField(idField, entity, id);
        } else {
            creator.invokeVirtualMethod(idSetter, entity, id);
        }
    }
}
