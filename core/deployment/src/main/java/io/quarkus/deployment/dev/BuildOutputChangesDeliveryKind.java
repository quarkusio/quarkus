package io.quarkus.deployment.dev;

/**
 * Describes whether an external build-output update contains a precise delta
 * or requests convergence from the complete current output trees.
 */
public enum BuildOutputChangesDeliveryKind {
    /**
     * Delivers the listed path changes relative to the preceding accepted
     * output baseline.
     */
    DELTA,
    /**
     * Requests that Quarkus discard delta assumptions and converge or restart
     * from the complete current output roots.
     */
    REBASELINE
}
