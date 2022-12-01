package io.quarkus.mongodb.panache;

import org.bson.types.ObjectId;

/**
 * Represents an entity with a generated ID field {@link #id} of type {@link ObjectId}. If your
 * Mongo entities extend this class they gain the ID field and auto-generated accessors
 * to all their public fields, as well as all the useful methods from {@link PanacheMongoEntityBase}.
 *
 * If you want a custom ID type or strategy, you can directly extend {@link PanacheMongoEntityBase}
 * instead, and write your own ID field. You will still get auto-generated accessors and
 * all the useful methods.
 *
 * @see PanacheMongoEntityBase
 */
public abstract class PanacheMongoEntity extends PanacheMongoEntityBase {

    /**
     * The auto-generated ID field.
     * This field is set by Mongo when this entity is persisted.
     *
     * @see #persist()
     */
    public ObjectId id;

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "<" + id + ">";
    }
}
