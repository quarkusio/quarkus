package io.quarkus.creator;

import io.quarkus.creator.config.Configurable;
import io.quarkus.creator.outcome.OutcomeProvider;

/**
 * A phase in the application build flow
 *
 * @author Alexey Loubyansky
 */
public interface AppCreationPhase<T extends AppCreationPhase<T>> extends Configurable<T>, OutcomeProvider<AppCreator> {
}
