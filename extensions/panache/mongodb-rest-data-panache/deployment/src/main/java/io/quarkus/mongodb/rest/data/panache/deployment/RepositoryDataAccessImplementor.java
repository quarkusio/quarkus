package io.quarkus.mongodb.rest.data.panache.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

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

    /**
     * Implements <code>repository.findAll().page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page) {
        ResultHandle panacheQuery = creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "findAll", PanacheQuery.class),
                getRepositoryInstance(creator));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), panacheQuery);
    }

    /**
     * Implements <code>repository.findAll(sort).page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort) {
        ResultHandle panacheQuery = creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "findAll", PanacheQuery.class, Sort.class),
                getRepositoryInstance(creator), sort);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), panacheQuery);
    }

    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle query, ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "find", PanacheQuery.class, String.class, Map.class),
                getRepositoryInstance(creator), query, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), panacheQuery);
    }

    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort, ResultHandle query,
            ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeInterfaceMethod(
                ofMethod(PanacheMongoRepositoryBase.class, "find", PanacheQuery.class, String.class, Sort.class, Map.class),
                getRepositoryInstance(creator), query, sort, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), panacheQuery);
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
    public ResultHandle pageCount(BytecodeCreator creator, ResultHandle page, ResultHandle query, ResultHandle queryParams) {
        ResultHandle panacheQuery = creator
                .invokeInterfaceMethod(ofMethod(PanacheMongoRepositoryBase.class, "find", PanacheQuery.class,
                        String.class, Map.class), getRepositoryInstance(creator), query, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "pageCount", int.class), panacheQuery);
    }

    @Override
    public ResultHandle pageCount(BytecodeCreator creator, ResultHandle page) {
        ResultHandle panacheQuery = creator
                .invokeInterfaceMethod(ofMethod(PanacheMongoRepositoryBase.class, "findAll", PanacheQuery.class),
                        getRepositoryInstance(creator));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "pageCount", int.class), panacheQuery);
    }

    private ResultHandle getRepositoryInstance(BytecodeCreator creator) {
        ResultHandle arcContainer = creator.invokeStaticMethod(ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = creator.invokeInterfaceMethod(
                ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class, Annotation[].class),
                arcContainer, creator.loadClassFromTCCL(repositoryClassName), creator.newArray(Annotation.class, 0));
        ResultHandle instance = creator.invokeInterfaceMethod(
                ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);

        creator.ifNull(instance).trueBranch()
                .throwException(RuntimeException.class, repositoryClassName + " instance was not found");

        return instance;
    }
}
