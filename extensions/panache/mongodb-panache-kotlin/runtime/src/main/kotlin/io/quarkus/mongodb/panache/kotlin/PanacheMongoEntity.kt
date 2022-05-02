package io.quarkus.mongodb.panache.kotlin

import org.bson.types.ObjectId

/**
 * Represents an entity with a generated ID field [id] of type [ObjectId]. If your
 * Mongo entities extend this class they gain the ID field and auto-generated accessors
 * to all their public fields, as well as all the useful methods from [PanacheMongoEntityBase].
 *
 * If you want a custom ID type or strategy, you can directly extend [PanacheMongoEntityBase]
 * instead, and write your own ID field. You will still get auto-generated accessors and
 * all the useful methods.
 *
 * @see PanacheMongoEntityBase
 */
abstract class PanacheMongoEntity : PanacheMongoEntityBase() {
    /**
     * The auto-generated ID field.
     * This field is set by Mongo when this entity is persisted.
     *
     * @see [PanacheMongoEntityBase.persist]
     */
    var id: ObjectId? = null

    /**
     * Default toString() implementation
     *
     * @return the class type and ID type
     */
    override fun toString(): String = "${this.javaClass.simpleName}<$id>"
}