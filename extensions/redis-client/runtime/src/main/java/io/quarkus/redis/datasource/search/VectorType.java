package io.quarkus.redis.datasource.search;

/**
 * Type of vector stored in a vector field.
 */
public enum VectorType {
    /**
     * A 32-bit floating point number.
     */
    FLOAT32,
    /**
     * A 64-bit floating point number.
     */
    FLOAT64
}
