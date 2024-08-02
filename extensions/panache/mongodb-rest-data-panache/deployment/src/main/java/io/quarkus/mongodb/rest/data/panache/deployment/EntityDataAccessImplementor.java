package io.quarkus.mongodb.rest.data.panache.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.List;
import java.util.Map;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

final class EntityDataAccessImplementor implements DataAccessImplementor {

    private final String entityClassName;

    EntityDataAccessImplementor(String entityClassName) {
        this.entityClassName = entityClassName;
    }

    @Override
    public ResultHandle findById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeStaticMethod(
                ofMethod(entityClassName, "findById", PanacheMongoEntityBase.class, Object.class), id);
    }

    /**
     * Implements <code>Entity.findAll().page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(ofMethod(entityClassName, "findAll", PanacheQuery.class));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), panacheQuery);
    }

    /**
     * Implements <code>Entity.findAll(sort).page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(
                ofMethod(entityClassName, "findAll", PanacheQuery.class, Sort.class),
                sort);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), panacheQuery);
    }

    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle query, ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(
                ofMethod(entityClassName, "find", PanacheQuery.class, String.class, Map.class),
                query, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), panacheQuery);
    }

    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort, ResultHandle query,
            ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(
                ofMethod(entityClassName, "find", PanacheQuery.class, String.class, Sort.class, Map.class),
                query, sort, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), panacheQuery);
    }

    @Override
    public ResultHandle persist(BytecodeCreator creator, ResultHandle entity) {
        creator.invokeVirtualMethod(ofMethod(entityClassName, "persist", void.class), entity);
        return entity;
    }

    @Override
    public ResultHandle persistOrUpdate(BytecodeCreator creator, ResultHandle entity) {
        creator.invokeVirtualMethod(ofMethod(entityClassName, "persistOrUpdate", void.class), entity);
        return entity;
    }

    @Override
    public ResultHandle deleteById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeStaticMethod(ofMethod(entityClassName, "deleteById", boolean.class, Object.class), id);
    }

    @Override
    public ResultHandle pageCount(BytecodeCreator creator, ResultHandle page, ResultHandle query, ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(ofMethod(entityClassName, "find", PanacheQuery.class,
                String.class, Map.class), query, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "pageCount", int.class), panacheQuery);
    }

    @Override
    public ResultHandle pageCount(BytecodeCreator creator, ResultHandle page) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(ofMethod(entityClassName, "findAll", PanacheQuery.class));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "pageCount", int.class), panacheQuery);
    }
}
