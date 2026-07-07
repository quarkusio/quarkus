package io.quarkus.deployment.dev;

/**
 * Describes how one path below an externally produced output root changed.
 */
public enum BuildOutputChangeKind {
    /**
     * The path did not exist in the preceding successful output state.
     */
    ADDED,
    /**
     * The path existed in both output states and its contents or metadata changed.
     */
    MODIFIED,
    /**
     * The path existed in the preceding output state and no longer exists.
     */
    DELETED
}
