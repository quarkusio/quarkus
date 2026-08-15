package io.quarkus.spring.data.rest.deployment;

import java.lang.annotation.Annotation;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.MethodDesc;

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

    default LocalVar getRepositoryInstance(BlockCreator bc, String repositoryInterface) {
        LocalVar arcContainer = bc.localVar("arcContainer",
                bc.invokeStatic(MethodDesc.of(Arc.class, "container", ArcContainer.class)));
        LocalVar instanceHandle = bc.localVar("instanceHandle", bc.invokeInterface(
                MethodDesc.of(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class),
                arcContainer, bc.classForName(Const.of(repositoryInterface)), bc.newArray(Annotation.class)));
        LocalVar instance = bc.localVar("instance", bc.invokeInterface(
                MethodDesc.of(InstanceHandle.class, "get", Object.class), instanceHandle));
        bc.if_(bc.isNull(instance), trueBranch -> {
            trueBranch.throw_(RuntimeException.class, repositoryInterface + " instance was not found");
        });

        return instance;
    }
}
