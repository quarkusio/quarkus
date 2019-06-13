package io.quarkus.creator.outcome;

import io.quarkus.creator.AppCreatorException;

/**
 * Phase registration callback.
 *
 * Allows phase handlers to declare what they consume and what they provide.
 *
 * @author Alexey Loubyansky
 */
public interface OutcomeProviderRegistration {

    /**
     * Invoked by a phase handler to declare it provides an outcome
     * of a specific type.
     *
     * @param outcomeType outcome type the handler provides
     * @throws AppCreatorException in case of a failure
     */
    void provides(Class<?> outcomeType) throws AppCreatorException;
}
