package io.quarkus.mongodb.rest.data.panache.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

final class RepositoryDataAccessImplementor implements DataAccessImplementor {

    private final String repositoryClassName;

    RepositoryDataAccessImplementor(String repositoryClassName) {
        this.repositoryClassName = repositoryClassName;
    }

    @Override
    public ResultHandle findById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "findById", Object.class, Object.class),
                getRepositoryInstance(creator), id);
    }

    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page) {
        ResultHandle query = creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "findAll", PanacheQuery.class),
                getRepositoryInstance(creator));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), query,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), query);
    }

    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort) {
        ResultHandle query = creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "findAll", PanacheQuery.class, Sort.class),
                getRepositoryInstance(creator), sort);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), query,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), query);
    }

    @Override
    public ResultHandle persist(BytecodeCreator creator, ResultHandle entity) {
        creator.invokeInterfaceMethod(ofMethod(PanacheMongoRepositoryBase.class, "persist", void.class, Object.class),
                getRepositoryInstance(creator), entity);
        return entity;
    }

    @Override
    public ResultHandle persistOrUpdate(BytecodeCreator creator, ResultHandle entity) {
        creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "persistOrUpdate", void.class, Object.class),
                getRepositoryInstance(creator), entity);
        return entity;
    }

    @Override
    public ResultHandle deleteById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "deleteById", boolean.class, Object.class),
                getRepositoryInstance(creator), id);
    }

    @Override
    public ResultHandle pageCount(BytecodeCreator creator, ResultHandle page) {
        ResultHandle query = creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "findAll", PanacheQuery.class),
                getRepositoryInstance(creator));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), query,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "pageCount", int.class), query);
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
