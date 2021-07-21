package io.quarkus.mongodb.panache.common;

import java.util.Map;

import io.quarkus.panache.common.Parameters;

/**
 * Interface representing an update query.
 *
 * Use one of its methods to perform the update query.
 */
public interface PanacheUpdate extends io.quarkus.mongodb.panache.PanacheUpdate {

    /**
     * Execute the update query with the update document.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params params optional sequence of indexed parameters
     * @return the number of entities updated.
     */
    public long where(String query, Object... params);

    /**
     * Execute the update query with the update document.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities updated.
     */
    public long where(String query, Map<String, Object> params);

    /**
     * Execute the update query with the update document.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities updated.
     */
    public long where(String query, Parameters params);

    /**
     * Execute an update on all documents with the update document.
     *
     * @return the number of entities updated.
     */
    public long all();
}
