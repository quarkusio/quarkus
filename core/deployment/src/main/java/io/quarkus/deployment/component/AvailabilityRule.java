package io.quarkus.deployment.component;

import java.util.List;

import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * A single check that may prevent a component from being available.
 * <p>
 * Multiple rules are composed into a {@link ComponentLookup}
 * via {@link ComponentLookup#of(List)}.
 *
 * @see ComponentLookup
 */
@FunctionalInterface
public interface AvailabilityRule {

    List<Reason> unavailableReasons(ProgrammingParadigm paradigm, String name);

}
