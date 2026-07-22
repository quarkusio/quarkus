package io.quarkus.deployment.dev;

/**
 * Identifies which externally owned build outputs failed to be produced.
 */
public enum BuildOutputFailureKind {
    NONE,
    MAIN,
    TEST,
    UNKNOWN
}
