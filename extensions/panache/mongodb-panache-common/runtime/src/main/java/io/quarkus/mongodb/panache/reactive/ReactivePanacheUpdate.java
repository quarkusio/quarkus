package io.quarkus.mongodb.panache.reactive;

import java.util.Map;

import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;

/**
 * Interface representing an update query.
 *
 * Use one of its methods to perform the update query.
 *
 * @deprecated use {@link io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate} instead.
 */
@Deprecated(forRemoval = true, since = "2.1.0")
public interface ReactivePanacheUpdate {
    /**
     * Execute the update query with the update document.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params params optional sequence of indexed parameters
     * @return the number of entities updated.
     */
    public Uni<Long> where(String query, Object... params);

    /**
     * Execute the update query with the update document.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return the number of entities updated.
     */
    public Uni<Long> where(String query, Map<String, Object> params);

    /**
     * Execute the update query with the update document.
     *
     * @param query a {@link io.quarkus.mongodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return the number of entities updated.
     */
    public Uni<Long> where(String query, Parameters params);

    /**
     * Execute an update on all documents with the update document.
     *
     * @return the number of entities updated.
     */
    public Uni<Long> all();
}
