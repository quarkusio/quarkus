package io.quarkus.hibernate.panache.stateless.blocking;

import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.StatelessSession;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.stateless.PanacheStatelessRepositoryOperations;

public interface PanacheStatelessBlockingRepositoryOperations<Entity, Id>
        extends PanacheStatelessRepositoryOperations<Entity, StatelessSession, Void, Boolean, Id> {

    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingStateless();
    }

    // Operations

    /**
     * Returns the {@link StatelessSession} for the <Entity> entity class for extra operations (eg. CriteriaQueries)
     *
     * @return the {@link StatelessSession} for the <Entity> entity class
     */
    default StatelessSession getSession() {
        return operations().getStatelessSession(getEntityClass());
    }

    /**
     * Insert the given entity in the database, if not already inserted.
     *
     * @param entity the entity to insert.
     * @see #insert(Iterable)
     * @see #insert(Stream)
     * @see #insert(Object, Object...)
     */
    default Void insert(Entity entity) {
        return operations().insert(entity);
    }

    /**
     * Delete the given entity from the database, if it is already inserted.
     *
     * @param entity the entity to delete.
     * @see #isInsertent(Object)
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #deleteAll()
     */
    default Void delete(Entity entity) {
        return operations().delete(entity);
    }

    /**
     * Update the given entity in the database.
     *
     * @param entity the entity to update.
     */
    default Void update(Entity entity) {
        return operations().update(entity);
    }

    /**
     * Insert or update this entity in the database. An insert will be performed if the entity does not already exist
     * in the database, otherwise it will be updated. Note that you cannot upsert an entity with a null ID.
     *
     * @param entity the entity to insert or update.
     */
    default Void upsert(Entity entity) {
        return operations().upsert(entity);
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #insert(Object)
     * @see #insert(Stream)
     * @see #insert(Object,Object...)
     */
    default Void insert(Iterable<Entity> entities) {
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
    default Void insert(Stream<Entity> entities) {
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
    default Void insert(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().insert(firstEntity, entities);
    }
}
