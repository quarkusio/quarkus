package io.quarkus.runtime.util;

/**
 * An enum representing the blocking/reactive split, for example in APIs or in configuration.
 * <p>
 * Useful to merge code paths that are mostly identical for blocking/reactive, but diverge in some small aspects,
 * without relying on awfully undescriptive booleans.
 */
public enum ProgrammingParadigm {

    BLOCKING,
    REACTIVE

}
