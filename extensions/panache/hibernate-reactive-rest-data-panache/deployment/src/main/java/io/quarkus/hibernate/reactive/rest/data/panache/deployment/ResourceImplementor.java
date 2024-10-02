package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.logging.Logger;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.deployment.Constants;
import io.quarkus.runtime.util.HashUtil;
import io.smallrye.mutiny.Uni;

/**
 * {@link io.quarkus.rest.data.panache.ReactiveRestDataResource} implementor that generates data access logic depending on which
 * sub-interfaces are used in the application.
 * The method implementation differs depending on a data access strategy (active record or repository).
 */
class ResourceImplementor {

    private static final Logger LOGGER = Logger.getLogger(ResourceImplementor.class);

    private final EntityClassHelper entityClassHelper;

    ResourceImplementor(EntityClassHelper entityClassHelper) {
        this.entityClassHelper = entityClassHelper;
    }

    /**
     * Implements {@link io.quarkus.rest.data.panache.ReactiveRestDataResource} interfaces defined in a user application.
     * Instances of this class are registered as beans and are later used in the generated JAX-RS controllers.
     */
    String implement(ClassOutput classOutput, DataAccessImplementor dataAccessImplementor, ClassInfo resourceInterface,
            String entityType, List<ClassInfo> resourceMethodListeners) {
        String resourceType = resourceInterface.name().toString();
        String className = resourceType + "Impl_" + HashUtil.sha1(resourceType);
        LOGGER.tracef("Starting generation of '%s'", className);
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(className)
                .interfaces(resourceType)
                .build();

        classCreator.addAnnotation(ApplicationScoped.class);
        // The same resource is generated as part of the JaxRsResourceImplementor, so we need to avoid ambiguous resolution
        // when injecting the resource in user beans:
        classCreator.addAnnotation(Alternative.class);
        classCreator.addAnnotation(Priority.class).add("value", Integer.MAX_VALUE);
        classCreator.addAnnotation(WithSessionOnDemand.class);

        HibernateReactiveResourceMethodListenerImplementor resourceMethodListenerImplementor = new HibernateReactiveResourceMethodListenerImplementor(
                classCreator, resourceMethodListeners);

        implementList(classCreator, dataAccessImplementor);
        implementListWithQuery(classCreator, dataAccessImplementor);
        implementListPageCount(classCreator, dataAccessImplementor);
        implementCount(classCreator, dataAccessImplementor);
        implementGet(classCreator, dataAccessImplementor);
        implementAdd(classCreator, dataAccessImplementor, resourceMethodListenerImplementor);
        implementUpdate(classCreator, dataAccessImplementor, entityType, resourceMethodListenerImplementor);
        implementDelete(classCreator, dataAccessImplementor, resourceMethodListenerImplementor);

        classCreator.close();
        LOGGER.tracef("Completed generation of '%s'", className);
        return className;
    }

    private void implementList(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("list", Uni.class, Page.class, Sort.class);
        ResultHandle page = methodCreator.getMethodParam(0);
        ResultHandle sort = methodCreator.getMethodParam(1);
        ResultHandle columns = methodCreator.invokeVirtualMethod(ofMethod(Sort.class, "getColumns", List.class), sort);
        ResultHandle isEmptySort = methodCreator.invokeInterfaceMethod(ofMethod(List.class, "isEmpty", boolean.class), columns);

        BranchResult isEmptySortBranch = methodCreator.ifTrue(isEmptySort);
        isEmptySortBranch.trueBranch().returnValue(dataAccessImplementor.findAll(isEmptySortBranch.trueBranch(), page));
        isEmptySortBranch.falseBranch().returnValue(dataAccessImplementor.findAll(isEmptySortBranch.falseBranch(), page, sort));

        methodCreator.close();
    }

    private void implementListWithQuery(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("list", Uni.class, Page.class, Sort.class,
                String.class, Map.class);
        ResultHandle page = methodCreator.getMethodParam(0);
        ResultHandle sort = methodCreator.getMethodParam(1);
        ResultHandle query = methodCreator.getMethodParam(2);
        ResultHandle queryParams = methodCreator.getMethodParam(3);
        ResultHandle columns = methodCreator.invokeVirtualMethod(ofMethod(Sort.class, "getColumns", List.class), sort);
        ResultHandle isEmptySort = methodCreator.invokeInterfaceMethod(ofMethod(List.class, "isEmpty", boolean.class), columns);

        BranchResult isEmptySortBranch = methodCreator.ifTrue(isEmptySort);
        isEmptySortBranch.trueBranch().returnValue(dataAccessImplementor.findAll(isEmptySortBranch.trueBranch(), page, query,
                queryParams));
        isEmptySortBranch.falseBranch().returnValue(dataAccessImplementor.findAll(isEmptySortBranch.falseBranch(), page, sort,
                query, queryParams));

        methodCreator.close();
    }

    /**
     * Generate count method.
     */
    private void implementCount(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("count", Uni.class);
        methodCreator.returnValue(dataAccessImplementor.count(methodCreator));
        methodCreator.close();
    }

    /**
     * Generate list page count method.
     * This method is used when building page URLs for list operation response and is not exposed to a user.
     */
    private void implementListPageCount(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator(Constants.PAGE_COUNT_METHOD_PREFIX + "list", Uni.class,
                Page.class, String.class, Map.class);
        ResultHandle page = methodCreator.getMethodParam(0);
        ResultHandle query = methodCreator.getMethodParam(1);
        ResultHandle queryParams = methodCreator.getMethodParam(2);
        methodCreator.returnValue(dataAccessImplementor.pageCount(methodCreator, page, query, queryParams));
        methodCreator.close();
    }

    private void implementGet(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("get", Uni.class, Object.class);
        ResultHandle id = methodCreator.getMethodParam(0);
        methodCreator.returnValue(dataAccessImplementor.findById(methodCreator, id));
        methodCreator.close();
    }

    private void implementAdd(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor,
            HibernateReactiveResourceMethodListenerImplementor resourceMethodListenerImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("add", Uni.class, Object.class);
        methodCreator.addAnnotation(WithTransaction.class);
        ResultHandle entity = methodCreator.getMethodParam(0);
        resourceMethodListenerImplementor.onBeforeAdd(methodCreator, entity);
        ResultHandle uni = dataAccessImplementor.persist(methodCreator, entity);
        uni = resourceMethodListenerImplementor.onAfterAdd(methodCreator, uni);
        methodCreator.returnValue(uni);
        methodCreator.close();
    }

    private void implementUpdate(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor, String entityType,
            HibernateReactiveResourceMethodListenerImplementor resourceMethodListenerImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("update", Uni.class, Object.class, Object.class);
        methodCreator.addAnnotation(WithTransaction.class);
        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entity = methodCreator.getMethodParam(1);
        // Set entity ID before executing an update to make sure that a requested object ID matches a given entity ID.
        setId(methodCreator, entityType, entity, id);
        resourceMethodListenerImplementor.onBeforeUpdate(methodCreator, entity);
        ResultHandle uni = dataAccessImplementor.update(methodCreator, entity);
        uni = resourceMethodListenerImplementor.onAfterUpdate(methodCreator, uni);
        methodCreator.returnValue(uni);
        methodCreator.close();
    }

    private void implementDelete(ClassCreator classCreator, DataAccessImplementor dataAccessImplementor,
            HibernateReactiveResourceMethodListenerImplementor resourceMethodListenerImplementor) {
        MethodCreator methodCreator = classCreator.getMethodCreator("delete", Uni.class, Object.class);
        methodCreator.addAnnotation(WithTransaction.class);
        ResultHandle id = methodCreator.getMethodParam(0);
        resourceMethodListenerImplementor.onBeforeDelete(methodCreator, id);
        ResultHandle uni = dataAccessImplementor.deleteById(methodCreator, id);
        uni = resourceMethodListenerImplementor.onAfterDelete(methodCreator, uni, id);
        methodCreator.returnValue(uni);
        methodCreator.close();
    }

    private void setId(BytecodeCreator creator, String entityType, ResultHandle entity, ResultHandle id) {
        FieldInfo idField = entityClassHelper.getIdField(entityType);
        MethodDescriptor idSetter = entityClassHelper.getSetter(entityType, idField);
        creator.invokeVirtualMethod(idSetter, entity, id);
    }
}
