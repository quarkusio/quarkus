package io.quarkus.mongodb.panache;

import org.bson.types.ObjectId;

/**
 * Represents a Repository for a specific type of entity {@code Entity}, with an ID type
 * of {@code ObjectId}. Implementing this repository will gain you the exact same useful methods
 * that are on {@link PanacheMongoEntityBase}. If you have a custom ID strategy, you should
 * implement {@link PanacheMongoRepositoryBase} instead.
 *
 * @param <Entity> The type of entity to operate on
 */
public interface PanacheMongoRepository<Entity> extends PanacheMongoRepositoryBase<Entity, ObjectId> {

}
