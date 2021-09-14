package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.persistence.EntityManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

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
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "findById", Object.class, Object.class),
                getRepositoryInstance(creator), id);
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
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "list", List.class), query);
    }

    /**
     * Implements <code>repository.persist(entity)</code>
     */
    @Override
    public ResultHandle persist(BytecodeCreator creator, ResultHandle entity) {
        creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "persist", void.class, Object.class),
                getRepositoryInstance(creator), entity);
        return entity;
    }

    /**
     * Implements <code>JpaOperations.getEntityManager().merge(entity)</code>
     */
    @Override
    public ResultHandle update(BytecodeCreator creator, ResultHandle entity) {
        MethodDescriptor getEntityManager = ofMethod(PanacheRepositoryBase.class, "getEntityManager",
                EntityManager.class);
        ResultHandle entityManager = creator.invokeInterfaceMethod(getEntityManager, getRepositoryInstance(creator));
        return creator.invokeInterfaceMethod(
                ofMethod(EntityManager.class, "merge", Object.class, Object.class), entityManager, entity);
    }

    /**
     * Implements <code>repository.deleteById(id)</code>
     */
    @Override
    public ResultHandle deleteById(BytecodeCreator creator, ResultHandle id) {
        return creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "deleteById", boolean.class, Object.class),
                getRepositoryInstance(creator), id);
    }

    /**
     * Implements <code>repository.findAll().page(page).pageCount()</code>
     */
    @Override
    public ResultHandle pageCount(BytecodeCreator creator, ResultHandle page) {
        ResultHandle query = creator.invokeInterfaceMethod(ofMethod(PanacheRepositoryBase.class, "findAll", PanacheQuery.class),
                getRepositoryInstance(creator));
        creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class), query, page);
        return creator.invokeInterfaceMethod(ofMethod(PanacheQuery.class, "pageCount", int.class), query);
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
