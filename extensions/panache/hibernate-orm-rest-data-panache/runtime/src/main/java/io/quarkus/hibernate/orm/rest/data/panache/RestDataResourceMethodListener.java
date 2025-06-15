package io.quarkus.hibernate.orm.rest.data.panache;

/**
 * REST Data Resource method listener interface to subscribe to the pre-post events for each resource in REST Data with
 * Panache.
 *
 * @param <ENTITY>
 *        the entity to subscribe.
 */
public interface RestDataResourceMethodListener<ENTITY> {
    /**
     * This method is triggered before saving an entity.
     *
     * @param entity
     *        the entity to save.
     */
    default void onBeforeAdd(ENTITY entity) {
    }

    /**
     * Fired after saving an entity.
     *
     * @param entity
     *        the saved entity.
     */
    default void onAfterAdd(ENTITY entity) {
    }

    /**
     * Fired before updating an entity.
     *
     * @param entity
     *        the entity to update.
     */
    default void onBeforeUpdate(ENTITY entity) {
    }

    /**
     * Fired after updating an entity.
     *
     * @param entity
     *        the updated entity.
     */
    default void onAfterUpdate(ENTITY entity) {
    }

    /**
     * Fired before deleting an entity.
     *
     * @param id
     *        the entity id to delete.
     */
    default void onBeforeDelete(Object id) {
    }

    /**
     * Fired after deleting an entity.
     *
     * @param id
     *        of the deleted entity.
     */
    default void onAfterDelete(Object id) {
    }
}
