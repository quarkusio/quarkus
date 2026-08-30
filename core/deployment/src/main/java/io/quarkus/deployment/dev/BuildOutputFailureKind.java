package io.quarkus.deployment.dev;

/**
 * Identifies which externally owned build outputs failed to be produced.
 */
public enum BuildOutputFailureKind {
    /**
     * No build-output category failed.
     */
    NONE,
    /**
     * Main application outputs failed to compile or otherwise materialize.
     */
    MAIN,
    /**
     * Test outputs failed to compile or otherwise materialize.
     */
    TEST,
    /**
     * The producer could not classify the failed output category.
     */
    UNKNOWN
}
