package io.quarkus.mongodb.panache.kotlin.reactive

import org.bson.types.ObjectId

/**
 * Represents a Repository for a specific type of entity `Entity`, with an ID type
 * of `ObjectId`. Implementing this repository will gain you the exact same useful methods
 * that are on [ReactivePanacheMongoEntityBase]. If you have a custom ID strategy, you should
 * implement [ReactivePanacheMongoRepositoryBase] instead.
 *
 * @param Entity The type of entity to operate on
 */
interface ReactivePanacheMongoRepository<Entity: Any> : ReactivePanacheMongoRepositoryBase<Entity, ObjectId>