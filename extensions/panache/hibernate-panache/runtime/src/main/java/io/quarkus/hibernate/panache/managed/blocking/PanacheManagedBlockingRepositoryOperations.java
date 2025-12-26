package io.quarkus.hibernate.panache.managed.blocking;

import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.Session;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.panache.managed.PanacheManagedRepositoryOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheBlockingOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;

public interface PanacheManagedBlockingRepositoryOperations<Entity, Id>
        extends PanacheManagedRepositoryOperations<Entity, Session, Void, Boolean, Id> {

    private Class<? extends Entity> getEntityClass() {
        return AbstractJpaOperations.getRepositoryEntityClass(getClass());
    }

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingManaged();
    }

    // Operations

    /**
     * Returns the {@link Session} for the <Entity> entity class for extra operations (eg. CriteriaQueries)
     *
     * @return the {@link Session} for the <Entity> entity class
     */
    default Session getSession() {
        return operations().getSession(getEntityClass());
    }

    /**
     * Persist the given entity in the database, if not already persisted.
     *
     * @param entity the entity to persist.
     * @see #isPersistent(Object)
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    default Void persist(Entity entity) {
        return operations().persist(entity);
    }

    /**
     * Persist the given entity in the database, if not already persisted.
     * Then flushes all pending changes to the database.
     *
     * @param entity the entity to persist.
     * @see #isPersistent(Object)
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    default Void persistAndFlush(Entity entity) {
        return operations().persistAndFlush(entity);
    }

    /**
     * Delete the given entity from the database, if it is already persisted.
     *
     * @param entity the entity to delete.
     * @see #isPersistent(Object)
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #deleteAll()
     */
    default Void delete(Entity entity) {
        return operations().delete(entity);
    }

    /**
     * Returns true if the given entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @param entity the entity to check
     * @return true if the entity is persistent in the database.
     */
    default Boolean isPersistent(Entity entity) {
        return operations().isPersistent(entity);
    }

    /**
     * Flushes all pending changes to the database using the Session for the <Entity> entity class.
     */
    default Void flush() {
        return operations().flush(getEntityClass());
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see #persist(Object)
     * @see #persist(Stream)
     * @see #persist(Object,Object...)
     */
    default Void persist(Iterable<Entity> entities) {
        return operations().persist(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see #persist(Object)
     * @see #persist(Iterable)
     * @see #persist(Object,Object...)
     */
    default Void persist(Stream<Entity> entities) {
        return operations().persist(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see #persist(Object)
     * @see #persist(Stream)
     * @see #persist(Iterable)
     */
    default Void persist(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities) {
        return operations().persist(firstEntity, entities);
    }
}
