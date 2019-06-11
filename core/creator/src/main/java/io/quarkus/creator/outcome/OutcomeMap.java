package io.quarkus.creator.outcome;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.creator.AppCreatorException;

/**
 *
 * @author Alexey Loubyansky
 */
public class OutcomeMap {

    protected final OutcomeResolver<OutcomeMap> router;
    protected Map<Class<?>, Object> outcomes = new HashMap<>();

    public OutcomeMap(OutcomeResolver<OutcomeMap> router) {
        this.router = router;
    }

    @SuppressWarnings("unchecked")
    public <T> T resolveOutcome(Class<T> type) throws AppCreatorException {
        Object o = outcomes.get(type);
        if (o != null || outcomes.containsKey(type)) {
            return (T) o;
        }
        router.resolve(this, type);
        o = outcomes.get(type);
        if (o != null || outcomes.containsKey(type)) {
            return (T) o;
        }
        throw new AppCreatorException("Outcome of type " + type + " has not been provided");
    }

    public boolean isAvailable(Class<?> outcomeType) {
        return outcomes.containsKey(outcomeType);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOutcome(Class<T> type) {
        return (T) outcomes.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> void pushOutcome(T outcome) throws AppCreatorException {
        pushOutcome((Class<T>) outcome.getClass(), outcome);
    }

    public <T> void pushOutcome(Class<T> type, T value) throws AppCreatorException {
        if (outcomes.containsKey(type)) {
            throw new AppCreatorException("Outcome of type " + type.getName() + " has already been provided");
        }
        outcomes.put(type, value);
    }
}
