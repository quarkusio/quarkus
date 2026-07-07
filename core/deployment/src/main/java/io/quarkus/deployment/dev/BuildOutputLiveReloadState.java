package io.quarkus.deployment.dev;

/**
 * Monotonic live-reload state published by a Quarkus dev-mode process to an
 * external build-output producer.
 *
 * @param generation monotonically increasing state generation within the
 *        connection
 * @param enabled whether the receiver can currently apply live-reload updates
 */
public record BuildOutputLiveReloadState(long generation, boolean enabled) {

    /**
     * Creates and validates a published live-reload state.
     */
    public BuildOutputLiveReloadState {
        if (generation < 0) {
            throw new IllegalArgumentException("Live-reload state generation must not be negative");
        }
    }
}
