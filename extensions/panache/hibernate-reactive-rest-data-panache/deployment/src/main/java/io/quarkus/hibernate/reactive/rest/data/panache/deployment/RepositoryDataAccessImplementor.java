package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;
import io.smallrye.mutiny.Uni;

/**
 * Implement data access using repository.
 */
final class RepositoryDataAccessImplementor implements DataAccessImplementor {

    private final String repositoryClassName;

    RepositoryDataAccessImplementor(String repositoryClassName) {
        this.repositoryClassName = repositoryClassName;
    }

    /**
     * Implements <code>repository.findById(id)</code>
     */
    @Override
    public ResultHandle findById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "findById", Uni.class, Object.class),
                getRepositoryInstance(creator), id);
    }

    /**
     * Implements <code>repository.findAll().page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page) {
        ResultHandle query = creator.invokeInterfaceMethod(
                ofMethod(PanacheRepositoryBase.class, "findAll", PanacheQuery.class), getRepositoryInstance(creator));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), query, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", Uni.class), query);
    }

    /**
     * Implements <code>repository.findAll(sort).page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort) {
        ResultHandle query = creator.invokeInterfaceMethod(
                ofMethod(PanacheRepositoryBase.class, "findAll", PanacheQuery.class, Sort.class),
                getRepositoryInstance(creator), sort);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), query, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", Uni.class), query);
    }

    /**
     * Implements <code>repository.find(query, params).page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle query, ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeInterfaceMethod(
                ofMethod(PanacheRepositoryBase.class, "find", PanacheQuery.class, String.class, Map.class),
                getRepositoryInstance(creator), query, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", Uni.class), panacheQuery);
    }

    /**
     * Implements <code>repository.findAll(query, sort, params).page(page).list()</code>
     */
    @Override
    public ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort, ResultHandle query,
            ResultHandle queryParams) {
        ResultHandle panacheQuery = creator.invokeInterfaceMethod(
                ofMethod(PanacheRepositoryBase.class, "find", PanacheQuery.class, String.class, Sort.class, Map.class),
                getRepositoryInstance(creator), query, sort, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", Uni.class), panacheQuery);
    }

    /**
     * Implements <code>repository.persist(entity)</code>
     */
    @Override
    public ResultHandle persist(BytecodeCreator creator, ResultHandle entity) {
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "persist", Uni.class, Object.class),
                getRepositoryInstance(creator), entity);
    }

    /**
     * Persist this entity in the database, if not already persisted. This will set your ID field if it is not already set.
     *
     * @return
     *
     * @see Mutiny.Session#merge(Object)
     */
    @Override
    public ResultHandle update(BytecodeCreator creator, ResultHandle entity) {
        ResultHandle uniSession = creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "getSession", Uni.class),
                getRepositoryInstance(creator));
        return UniImplementor.flatMap(creator, uniSession, "Failed to retrieve session",
                (body, session) -> body.returnValue(
                        body.invokeInterfaceMethod(ofMethod(Mutiny.Session.class, "merge", Uni.class, Object.class),
                                session, entity)));
    }

    /**
     * Implements <code>repository.deleteById(id)</code>
     */
    @Override
    public ResultHandle deleteById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "deleteById", Uni.class, Object.class),
                getRepositoryInstance(creator), id);
    }

    /**
     * Implements <code>repository.find(query, params).page(page).pageCount()</code>
     */
    @Override
    public ResultHandle pageCount(BytecodeCreator creator, ResultHandle page, ResultHandle query, ResultHandle queryParams) {
        ResultHandle panacheQuery = creator
                .invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "find", PanacheQuery.class,
                        String.class, Map.class), getRepositoryInstance(creator), query, queryParams);
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), panacheQuery, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "pageCount", Uni.class), panacheQuery);
    }

    /**
     * Implements <code>repository.count()</code>
     */
    @Override
    public ResultHandle count(BytecodeCreator creator) {
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "count", Uni.class),
                getRepositoryInstance(creator));
    }

    /**
     * Implements getting repository from Arc container.
     * <code>Arc.container().instance(Repository.class, new Annotation[0]).get()</code>
     */
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
