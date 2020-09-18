package io.quarkus.spring.data.rest.deployment.crud;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.springframework.data.repository.CrudRepository;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.deployment.Constants;
import io.quarkus.spring.data.rest.deployment.ResourceMethodsImplementor;

public class CrudMethodsImplementor implements ResourceMethodsImplementor {

    public static final MethodDescriptor LIST = ofMethod(CrudRepository.class, "findAll", Iterable.class);

    public static final MethodDescriptor GET = ofMethod(CrudRepository.class, "findById", Optional.class, Object.class);

    public static final MethodDescriptor ADD = ofMethod(CrudRepository.class, "save", Object.class, Object.class);

    public static final MethodDescriptor UPDATE = ofMethod(CrudRepository.class, "save", Object.class, Object.class);

    public static final MethodDescriptor DELETE = ofMethod(CrudRepository.class, "deleteById", void.class, Object.class);

    private final EntityClassHelper entityClassHelper;

    public CrudMethodsImplementor(IndexView index) {
        this.entityClassHelper = new EntityClassHelper(index);
    }

    public void implementList(ClassCreator classCreator, String repositoryInterface) {
        MethodCreator methodCreator = classCreator.getMethodCreator("list", List.class, Page.class, Sort.class);

        ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterface);
        ResultHandle result = methodCreator.invokeInterfaceMethod(LIST, repository);

        methodCreator.returnValue(result);
        methodCreator.close();
    }

    public void implementListPageCount(ClassCreator classCreator, String repositoryInterface) {
        MethodCreator methodCreator = classCreator.getMethodCreator(Constants.PAGE_COUNT_METHOD_PREFIX + "list",
                int.class, Page.class);
        methodCreator.throwException(RuntimeException.class, "Method not implemented");
        methodCreator.close();
    }

    public void implementGet(ClassCreator classCreator, String repositoryInterface) {
        MethodCreator methodCreator = classCreator.getMethodCreator("get", Object.class, Object.class);

        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterface);
        ResultHandle result = findById(methodCreator, id, repository);

        methodCreator.returnValue(result);
        methodCreator.close();
    }

    public void implementAdd(ClassCreator classCreator, String repositoryInterface) {
        MethodCreator methodCreator = classCreator.getMethodCreator("add", Object.class, Object.class);

        ResultHandle entity = methodCreator.getMethodParam(0);
        ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterface);
        ResultHandle result = methodCreator.invokeInterfaceMethod(ADD, repository, entity);

        methodCreator.returnValue(result);
        methodCreator.close();
    }

    public void implementUpdate(ClassCreator classCreator, String repositoryInterface, String entityType) {
        MethodCreator methodCreator = classCreator.getMethodCreator("update", Object.class, Object.class, Object.class);

        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle entity = methodCreator.getMethodParam(1);
        // Set entity ID before executing an update to make sure that a requested object ID matches a given entity ID.
        setId(methodCreator, entityType, entity, id);
        ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterface);
        ResultHandle result = methodCreator.invokeInterfaceMethod(UPDATE, repository, entity);

        methodCreator.returnValue(result);
        methodCreator.close();
    }

    public void implementDelete(ClassCreator classCreator, String repositoryInterface) {
        MethodCreator methodCreator = classCreator.getMethodCreator("delete", boolean.class, Object.class);

        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle repository = getRepositoryInstance(methodCreator, repositoryInterface);
        ResultHandle entity = findById(methodCreator, id, repository);
        AssignableResultHandle result = methodCreator.createVariable(boolean.class);
        BranchResult entityExists = methodCreator.ifNotNull(entity);
        entityExists.trueBranch().invokeInterfaceMethod(DELETE, repository, id);
        entityExists.trueBranch().assign(result, entityExists.trueBranch().load(true));
        entityExists.falseBranch().assign(result, entityExists.falseBranch().load(false));

        methodCreator.returnValue(result);
        methodCreator.close();
    }

    private ResultHandle findById(BytecodeCreator creator, ResultHandle id, ResultHandle repository) {
        ResultHandle optional = creator.invokeInterfaceMethod(GET, repository, id);
        return creator.invokeVirtualMethod(ofMethod(Optional.class, "orElse", Object.class, Object.class),
                optional, creator.loadNull());
    }

    private void setId(BytecodeCreator creator, String entityType, ResultHandle entity, ResultHandle id) {
        FieldInfo idField = entityClassHelper.getIdField(entityType);
        MethodDescriptor idSetter = entityClassHelper.getSetter(entityType, idField);
        creator.invokeVirtualMethod(idSetter, entity, id);
    }

    protected ResultHandle getRepositoryInstance(BytecodeCreator creator, String repositoryInterface) {
        ResultHandle arcContainer = creator.invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = creator.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class),
                arcContainer, creator.loadClass(repositoryInterface), creator.newArray(Annotation.class, 0));
        ResultHandle instance = creator.invokeInterfaceMethod(
                ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);
        creator.ifNull(instance)
                .trueBranch()
                .throwException(RuntimeException.class, repositoryInterface + " instance was not found");

        return instance;
    }
}
