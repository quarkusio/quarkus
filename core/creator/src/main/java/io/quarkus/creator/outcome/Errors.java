package io.quarkus.creator.outcome;

/**
 *
 * @author Alexey Loubyansky
 */
public class Errors {

    public static String alternativeOutcomeProviders(Class<?> outcomeType, OutcomeProvider<?> phase1,
            OutcomeProvider<?> phase2) {
        return "Outcome of type " + outcomeType.getName() + " is provided by two phases: " + phase1.getClass().getName()
                + " and " + phase2.getClass().getName();
    }

    public static String promisedOutcomeNotProvided(OutcomeProvider<?> handler, Class<?> outcomeType) {
        return "Phase " + handler.getClass().getName() + " has not provided outcome of type " + outcomeType.getName();
    }

    public static String circularPhaseDependency(Class<?> outcomeType, OutcomeProvider<?> handler) {
        return "Failed to resolve outcome of type " + outcomeType.getName()
                + " due to circular phase dependencies originating from phase " + handler.getClass().getName();
    }

    public static String noProviderForOutcome(Class<?> outcomeType) {
        return "No phase found providing outcome of type " + outcomeType.getName();
    }
}
