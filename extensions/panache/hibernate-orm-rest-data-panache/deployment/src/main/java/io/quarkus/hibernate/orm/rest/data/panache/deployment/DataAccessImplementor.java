package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

/**
 * Implement data access logic for a specific data access strategy.
 */
public interface DataAccessImplementor {

    /**
     * Find an entity by ID and return a result.
     *
     * @param creator Bytecode creator that should be used for implementation.
     * @param id Requested entity ID.
     * @return A requested entity or null if it wasn't found.
     */
    ResultHandle findById(BytecodeCreator creator, ResultHandle id);

    /**
     * Find all entities.
     *
     * @param creator Bytecode creator that should be used for implementation.
     * @param page Page instance that should be used in a query. Might be null if pagination is disabled.
     * @return Entity list
     */
    ResultHandle findAll(BytecodeCreator creator, ResultHandle page);

    /**
     * Find all entities.
     *
     * @param creator Bytecode creator that should be used for implementation.
     * @param page Page instance that should be used in a query. Might be null if pagination is disabled.
     * @param sort Sort instance that should be used in a query.
     * @return Entity list
     */
    ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort);

    /**
     * Persist a new entity.
     *
     * @param creator Bytecode creator that should be used for implementation.
     * @param entity An entity that should be persisted.
     * @return A persisted entity.
     */
    ResultHandle persist(BytecodeCreator creator, ResultHandle entity);

    /**
     * Update an existing entity or create a new one.
     *
     * @param creator Bytecode creator that should be used for implementation.
     * @param entity Entity that should be updated or created.
     * @return A persisted entity.
     */
    ResultHandle update(BytecodeCreator creator, ResultHandle entity);

    /**
     * Delete entity by ID.
     *
     * @param creator Bytecode creator that should be used for implementation.
     * @param id Entity ID.
     * @return Boolean indicating whether an entity was deleted or not.
     */
    ResultHandle deleteById(BytecodeCreator creator, ResultHandle id);

    /**
     * Available number of pages given a page instance.
     *
     * @param creator Bytecode creator that should be used for implementation.
     * @param page Page instance.
     * @return int page count.
     */
    ResultHandle pageCount(BytecodeCreator creator, ResultHandle page);
}
