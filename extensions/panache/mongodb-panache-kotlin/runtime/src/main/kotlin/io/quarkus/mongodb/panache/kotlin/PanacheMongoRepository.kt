package io.quarkus.mongodb.panache.kotlin

import org.bson.types.ObjectId

/**
 * Represents a Repository for a specific type of entity `Entity`, with an ID type
 * of `ObjectId`. Implementing this repository will gain you the exact same useful methods
 * that are on [PanacheMongoEntityBase]. If you have a custom ID strategy, you should
 * implement [PanacheMongoRepositoryBase] instead.
 *
 * @param Entity The type of entity to operate on
 */
interface PanacheMongoRepository<Entity: Any> : PanacheMongoRepositoryBase<Entity, ObjectId>