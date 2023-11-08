package io.quarkus.redis.datasource.search;

/**
 * The vector algorithm to use when searching k most similar vectors in an index.
 */
public enum VectorAlgorithm {

    /**
     * Brute force algorithm.
     */
    FLAT,

    /**
     * Hierarchical Navigable Small World Graph algorithm.
     */
    HNSW

}
