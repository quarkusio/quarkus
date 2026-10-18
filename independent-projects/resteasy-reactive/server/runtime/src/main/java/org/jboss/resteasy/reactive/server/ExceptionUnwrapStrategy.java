package org.jboss.resteasy.reactive.server;

/**
 * Defines the strategy for unwrapping exceptions during exception handling.
 */
public enum ExceptionUnwrapStrategy {
    /**
     * Always unwraps exception of this type before checking mappers.
     * Only falls back to checking mappers for the wrapper exception if no mapper is found for any unwrapped cause.
     */
    ALWAYS,

    /**
     * Unwraps exception only if the thrown exception type itself has no exact mapper registered.
     * Checks for an exact match first, then unwraps if none is found.
     */
    UNWRAP_IF_NO_EXACT_MATCH,

    /**
     * Unwraps exceptions only if neither the thrown exception nor any of its supertypes have a registered mapper.
     * Checks the entire type hierarchy before unwrapping.
     */
    UNWRAP_IF_NO_MATCH
}
