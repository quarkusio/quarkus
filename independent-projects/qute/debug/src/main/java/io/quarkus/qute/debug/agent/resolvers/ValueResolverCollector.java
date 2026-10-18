package io.quarkus.qute.debug.agent.resolvers;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;

/**
 * Interface to collect information from a {@link ValueResolver} for debugging
 * purposes.
 * <p>
 * Implementations decide whether a given {@link ValueResolver} is applicable
 * to a specific evaluation context, and how to collect values into a
 * {@link ValueResolverContext}.
 */
public interface ValueResolverCollector {

    /**
     * Determines if the given {@link ValueResolver} is applicable in the provided
     * {@link EvalContext}.
     *
     * @param valueResolver the value resolver to test
     * @param evalContext the evaluation context for the current expression
     * @return {@code true} if the resolver can be applied, {@code false} otherwise
     */
    boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext);

    /**
     * Collects the values from the given {@link ValueResolver} into the provided
     * {@link ValueResolverContext} for debugging.
     *
     * @param valueResolver the value resolver to collect values from
     * @param context the context to store collected values
     */
    void collect(ValueResolver valueResolver, ValueResolverContext context);
}
