package io.quarkus.creator.outcome;

import io.quarkus.creator.AppCreatorException;

/**
 * Phase handler
 *
 * @author Alexey Loubyansky
 */
public interface OutcomeProvider<C> {

    /**
     * Invoked during when a handler is added to the phase router.
     * This method allows to register consumed and provided outcome types.
     *
     * @param registration registration callback
     * @throws AppCreatorException in case of a failure
     */
    void register(OutcomeProviderRegistration registration) throws AppCreatorException;

    /**
     * Invoked by the router to process the phase.
     *
     * @param ctx phase processing context
     * @throws AppCreatorException in case of a failure
     */
    void provideOutcome(C ctx) throws AppCreatorException;
}
