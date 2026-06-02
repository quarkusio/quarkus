package io.quarkus.hibernate.panache.managed;

import java.util.stream.Stream;

public interface PanacheManagedRepositoryOperations<Entity, Session, Completion, Confirmation, Id> {

    // Operations

    /**
     * Returns the {@link Session} for the <Entity> entity class for extra operations (eg. CriteriaQueries)
     *
     * @return the {@link Session} for the <Entity> entity class
     */
    Session getSession();

    /**
     * Persist the given entity in the database, if not already persisted.
     *
     * @param entity the entity to persist.
     * @see #isPersistent(Object)
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    Completion persist(Entity entity);

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
    Completion persistAndFlush(Entity entity);

    /**
     * Delete the given entity from the database, if it is already persisted.
     *
     * @param entity the entity to delete.
     * @see #isPersistent(Object)
     */
    Completion delete(Entity entity);

    /**
     * Returns true if the given entity is persistent in the database. If yes, all modifications to
     * its persistent fields will be automatically committed to the database at transaction
     * commit time.
     *
     * @param entity the entity to check
     * @return true if the entity is persistent in the database.
     */
    Confirmation isPersistent(Entity entity);

    /**
     * Flushes all pending changes to the database using the Session for the <Entity> entity class.
     */
    Completion flush();

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see #persist(Object)
     * @see #persist(Stream)
     * @see #persist(Object,Object...)
     */
    Completion persist(Iterable<Entity> entities);

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see #persist(Object)
     * @see #persist(Iterable)
     * @see #persist(Object,Object...)
     */
    Completion persist(Stream<Entity> entities);

    /**
     * Persist all given entities.
     *
     * @param entities the entities to persist
     * @see #persist(Object)
     * @see #persist(Stream)
     * @see #persist(Iterable)
     */
    Completion persist(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities);
}
