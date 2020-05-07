package io.quarkus.panache.rest.hibernate.orm.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.persistence.EntityManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.rest.common.deployment.DataAccessImplementor;

final class RepositoryDataAccessImplementor implements DataAccessImplementor {

    private final String repositoryClassName;

    RepositoryDataAccessImplementor(String repositoryClassName) {
        this.repositoryClassName = repositoryClassName;
    }

    @Override
    public ResultHandle findById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "findById", Object.class, Object.class),
                getRepositoryInstance(creator), id);
    }

    @Override
    public ResultHandle listAll(BytecodeCreator creator) {
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "listAll", List.class),
                getRepositoryInstance(creator));
    }

    @Override
    public ResultHandle persist(BytecodeCreator creator, ResultHandle entity) {
        creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "persist", void.class, Object.class),
                getRepositoryInstance(creator), entity);
        return entity;
    }

    @Override
    public ResultHandle update(BytecodeCreator creator, ResultHandle entity) {
        ResultHandle entityManager = creator.invokeStaticMethod(
                ofMethod(JpaOperations.class, "getEntityManager", EntityManager.class));
        return creator.invokeInterfaceMethod(
                ofMethod(EntityManager.class, "merge", Object.class, Object.class), entityManager, entity);
    }

    @Override
    public ResultHandle deleteById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "deleteById", boolean.class, Object.class),
                getRepositoryInstance(creator), id);
    }

    private ResultHandle getRepositoryInstance(BytecodeCreator creator) {
        ResultHandle arcContainer = creator.invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = creator.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class),
                arcContainer, creator.loadClass(repositoryClassName), creator.newArray(Annotation.class, 0));
        ResultHandle instance = creator.invokeInterfaceMethod(
                ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);

        creator.ifNull(instance).trueBranch()
                .throwException(RuntimeException.class, repositoryClassName + " instance was not found");

        return instance;
    }
}
