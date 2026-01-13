package io.quarkus.hibernate.panache.stateless.reactive;

import java.util.stream.Stream;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessRepositoryOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheStatelessReactiveRepositoryOperations<Entity, Id>
        extends PanacheStatelessRepositoryOperations<Entity, Uni<Mutiny.StatelessSession>, Uni<Void>, Uni<Boolean>, Id> {

    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveStateless();
    }

    // Operations

    /**
     * Returns the {@link Mutiny.StatelessSession} for the <Entity> entity class for extra operations (eg. CriteriaQueries)
     *
     * @return the {@link Mutiny.StatelessSession} for the <Entity> entity class
     */
    default Uni<Mutiny.StatelessSession> getSession() {
        // FIXME: this is false
        return operations().getStatelessSession(getEntityClass());
    }

    /**
     * Insert the given entity in the database.
     *
     * @param entity the entity to insert.
     */
    default Uni<Void> insert(Entity entity) {
        return operations().insert(entity);
    }

    /**
     * Delete the given entity from the database.
     *
     * @param entity the entity to delete.
     */
    default Uni<Void> delete(Entity entity) {
        return operations().delete(entity);
    }

    /**
     * Update the given entity in the database.
     *
     * @param entity the entity to update.
     */
    default Uni<Void> update(Entity entity) {
        return operations().update(entity);
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #insert(Object)
     * @see #insert(Stream)
     * @see #insert(Object,Object...)
     */
    default Uni<Void> insert(Iterable<Entity> entities) {
        return operations().insert(entities);
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #insert(Object)
     * @see #insert(Iterable)
     * @see #insert(Object,Object...)
     */
    default Uni<Void> insert(Stream<Entity> entities) {
        return operations().insert(entities);
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #insert(Object)
     * @see #insert(Stream)
     * @see #insert(Iterable)
     */
    default Uni<Void> insert(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().insert(firstEntity, entities);
    }
}
