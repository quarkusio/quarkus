package io.quarkus.deployment.dev;

/**
 * Outcome of one externally owned build iteration.
 */
public enum BuildOutputChangeStatus {
    /**
     * The build completed and its output state may be delivered.
     */
    BUILD_SUCCEEDED,
    /**
     * The build failed and does not establish a new accepted successful
     * baseline.
     */
    BUILD_FAILED,
    /**
     * The build was cancelled and does not establish a new accepted successful
     * baseline.
     */
    BUILD_CANCELLED,
    /**
     * A newer build iteration replaced this one, so it does not establish a
     * new accepted successful baseline.
     */
    BUILD_SUPERSEDED
}
