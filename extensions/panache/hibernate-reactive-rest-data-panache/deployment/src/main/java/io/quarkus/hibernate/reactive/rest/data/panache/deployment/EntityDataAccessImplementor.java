package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.Map;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;
import io.smallrye.mutiny.Uni;

/**
 * Implement data access using active record.
 */
final class EntityDataAccessImplementor implements DataAccessImplementor {

    private final String entityClassName;

    EntityDataAccessImplementor(String entityClassName) {
        this.entityClassName = entityClassName;
    }

    /**
     * Implements <code>Entity.findById(id)</code>
     */
    @Override
    public ResultHandle findById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeStaticMethod(ofMethod(entityClassName, "findById", Uni.class, Object.class),
                id);
    }

    /**
     * Implements <code>Entity.findAll().page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page) {
        ResultHandle query = creator.invokeStaticMethod(ofMethod(entityClassName, "findAll", PanacheQuery.class));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), query,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", Uni.class), query);
    }

    /**
     * Implements <code>Entity.findAll(sort).page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort) {
        ResultHandle query = creator.invokeStaticMethod(
                ofMethod(entityClassName, "findAll", PanacheQuery.class, Sort.class), sort);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), query,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", Uni.class), query);
    }

    /**
     * Implements <code>Entity.findAll(query, params).page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle query, ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(
                ofMethod(entityClassName, "find", PanacheQuery.class, String.class, Map.class),
                query, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", Uni.class), panacheQuery);
    }

    /**
     * Implements <code>Entity.findAll(query, sort, params).page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort, ResultHandle query,
            ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(
                ofMethod(entityClassName, "find", PanacheQuery.class, String.class, Sort.class, Map.class),
                query, sort, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", Uni.class), panacheQuery);
    }

    /**
     * Implements <code>entity.persist()</code>
     */
    @Override
    public ResultHandle persist(BytecodeCreator creator, ResultHandle entity) {
        return creator.invokeVirtualMethod(ofMethod(entityClassName, "persist", Uni.class), entity);
    }

    /**
     * Implements <code>entity.merge()</code>
     */
    @Override
    public ResultHandle update(BytecodeCreator creator, ResultHandle entity) {
        ResultHandle uniSession = creator.invokeStaticMethod(ofMethod(entityClassName, "getSession", Uni.class));
        return UniImplementor.flatMap(creator, uniSession, "Failed to retrieve session",
                (body, session) -> body.returnValue(
                        body.invokeInterfaceMethod(ofMethod(Mutiny.Session.class, "merge", Uni.class, Object.class),
                                session, entity)));
    }

    /**
     * Implements <code>Entity.deleteById(id)</code>
     */
    @Override
    public ResultHandle deleteById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeStaticMethod(ofMethod(entityClassName, "deleteById", Uni.class, Object.class), id);
    }

    /**
     * Implements <code>Entity.find(query, params).page(page).pageCount()</code>
     */
    @Override
    public ResultHandle pageCount(BytecodeCreator creator, ResultHandle page, ResultHandle query, ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeStaticMethod(ofMethod(entityClassName, "find", PanacheQuery.class,
                String.class, Map.class), query, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery,
                page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "pageCount", Uni.class), panacheQuery);
    }

    /**
     * Implements <code>Entity.count()</code>
     */
    @Override
    public ResultHandle count(BytecodeCreator creator) {
        return creator.invokeStaticMethod(ofMethod(entityClassName, "count", Uni.class));
    }
}
