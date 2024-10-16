package io.quarkus.spring.data.rest.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ResultHandle;

public interface ResourceMethodsImplementor {

    void implementList(ClassCreator classCreator, String repositoryInterface);

    void implementIterable(ClassCreator classCreator, String repositoryInterface);

    void implementPagedList(ClassCreator classCreator, String repositoryInterface);

    void implementListPageCount(ClassCreator classCreator, String repositoryInterface);

    void implementListById(ClassCreator classCreator, String repositoryInterface);

    void implementGet(ClassCreator classCreator, String repositoryInterface);

    void implementAdd(ClassCreator classCreator, String repositoryInterface);

    void implementAddList(ClassCreator classCreator, String repositoryInterface);

    void implementUpdate(ClassCreator classCreator, String repositoryInterface, String entityType);

    void implementDelete(ClassCreator classCreator, String repositoryInterface);

    default ResultHandle getRepositoryInstance(BytecodeCreator creator, String repositoryInterface) {
        ResultHandle arcContainer = creator.invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = creator.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class),
                arcContainer, creator.loadClassFromTCCL(repositoryInterface), creator.newArray(Annotation.class, 0));
        ResultHandle instance = creator.invokeInterfaceMethod(
                ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);
        creator.ifNull(instance)
                .trueBranch()
                .throwException(RuntimeException.class, repositoryInterface + " instance was not found");

        return instance;
    }
}
