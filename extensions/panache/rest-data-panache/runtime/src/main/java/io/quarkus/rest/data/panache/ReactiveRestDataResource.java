package io.quarkus.rest.data.panache;

import java.util.List;

import javax.ws.rs.core.Response;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

/**
 * Base REST Reactive data Panache resource interface.
 * Defines JAX-RS operations that will be implemented by the data store specific extensions such as Hibernate ORM or MongoDB.
 * <p>
 * User shouldn't use this interface directly but rather its sub-interfaces defined by the data store specific extensions.
 *
 * @param <Entity> Entity type that is handled by this resource.
 * @param <ID> ID type of the entity.
 */
public interface ReactiveRestDataResource<Entity, ID> {

    /**
     * Return entities as a JSON array.
     * The response is paged by default, but that could be disabled with {@link ResourceProperties} annotation.
     * Response content type: application/json.
     *
     * @param page Panache page instance that should be used in a query. Will be null if pagination is disabled.
     * @param sort Panache sort instance that should be used in a query.
     * @return A response with an entities JSON array.
     */
    Uni<List<Entity>> list(Page page, Sort sort);

    /**
     * Return an entity as a JSON object.
     * Response content type: application/json.
     *
     * @param id Entity identifier.
     * @return A response with a JSON object representing an entity.
     */
    Uni<Entity> get(ID id);

    /**
     * Create a new entity from the provided JSON object.
     * Request body type: application/json.
     * Response content type: application/json.
     *
     * @param entity Entity to be created
     * @return A response with a JSON object representing an entity and a location header of the new entity.
     */
    Uni<Entity> add(Entity entity);

    /**
     * Update an existing entity or create a new one from the provided JSON object.
     * Request content type: application/json
     * Response content type: application/json
     *
     * @param id Entity identifier.
     * @param entity Entity to be updated or created.
     * @return A response with no-content status in case of the update.
     *         A response with a JSON object representing an entity and a location header in case of the create.
     */
    Uni<Entity> update(ID id, Entity entity);

    /**
     * Delete an entity.
     *
     * @param id Entity identifier.
     * @return A boolean indicated whether the entity was deleted or not.
     */
    Uni<Boolean> delete(ID id);
}
