package io.quarkus.redis.datasource.search;

/**
 * Metric for computing the distance between two vectors.
 */
public enum DistanceMetric {
    L2,
    IP,
    COSINE
}
