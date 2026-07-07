package io.quarkus.deployment.dev;

/**
 * Result returned by Quarkus after receiving an external build-output update.
 */
public enum BuildOutputChangesApplyStatus {
    /**
     * The update was applied to the current dev-mode application state.
     */
    APPLIED,
    /**
     * The update was accepted by the transport but could not be applied.
     */
    NOT_APPLIED,
    /**
     * Live reload is currently disabled; the producer must retain or re-create
     * the update for later replay.
     */
    LIVE_RELOAD_DISABLED,
    /**
     * The update is invalid or incompatible with the current session and must
     * not be retried unchanged.
     */
    REJECTED
}
