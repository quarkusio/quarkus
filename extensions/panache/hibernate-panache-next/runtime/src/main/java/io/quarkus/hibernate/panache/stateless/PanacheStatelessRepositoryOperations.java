package io.quarkus.hibernate.panache.stateless;

import java.util.stream.Stream;

public interface PanacheStatelessRepositoryOperations<Entity, Session, CompletionEntity, Completion, Confirmation, Id> {

    // Operations

    /**
     * Returns the {@link Session} for the <Entity> entity class for extra operations (eg. CriteriaQueries)
     *
     * @return the {@link Session} for the <Entity> entity class
     */
    Session getSession();

    /**
     * Insert the given entity in the database.
     *
     * @param entity the entity to insert.
     * @return the entity passed as parameter
     * @see #insert(Iterable)
     * @see #insert(Stream)
     * @see #insert(Object, Object...)
     */
    CompletionEntity insert(Entity entity);

    /**
     * Delete the given entity from the database.
     *
     * @param entity the entity to delete.
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #deleteAll()
     * @return the entity passed as parameter
     */
    CompletionEntity delete(Entity entity);

    /**
     * Update the given entity in the database.
     *
     * @param entity the entity to update.
     * @return the entity passed as parameter
     */
    CompletionEntity update(Entity entity);

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #insert(Object)
     * @see #insert(Stream)
     * @see #insert(Object,Object...)
     */
    Completion insert(Iterable<Entity> entities);

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #insert(Object)
     * @see #insert(Iterable)
     * @see #insert(Object,Object...)
     */
    Completion insert(Stream<Entity> entities);

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #insert(Object)
     * @see #insert(Stream)
     * @see #insert(Iterable)
     */
    Completion insert(Entity firstEntity, @SuppressWarnings("unchecked") Entity... entities);
}
