package io.quarkus.mongodb.panache.reactive;

import org.bson.types.ObjectId;

/**
 * Represents a Repository for a specific type of entity {@code Entity}, with an ID type
 * of {@code ObjectId}. Implementing this repository will gain you the exact same useful methods
 * that are on {@link ReactivePanacheMongoEntityBase}. If you have a custom ID strategy, you should
 * implement {@link ReactivePanacheMongoRepositoryBase} instead.
 *
 * @param <Entity> The type of entity to operate on
 */
public interface ReactivePanacheMongoRepository<Entity> extends ReactivePanacheMongoRepositoryBase<Entity, ObjectId> {

}
