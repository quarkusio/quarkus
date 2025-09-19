package io.quarkus.qute.debug.agent.resolvers;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;

/**
 * Default implementation of {@link ValueResolverCollector} that collects
 * properties and methods supported by a {@link ValueResolver}.
 * <p>
 * Uses {@link ValueResolver#getSupportedProperties()} and
 * {@link ValueResolver#getSupportedMethods()} to populate the context.
 */
public class DefaultValueResolverCollector implements ValueResolverCollector {

    /**
     * Checks if the {@link ValueResolver} applies to the given evaluation context.
     *
     * @param valueResolver the value resolver to test
     * @param evalContext the evaluation context
     * @return {@code true} if the resolver applies, {@code false} otherwise
     */
    @Override
    public boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext) {
        return valueResolver.appliesTo(evalContext);
    }

    /**
     * Collects the supported properties and methods from the value resolver into
     * the provided {@link ValueResolverContext}.
     * <p>
     * Only collects properties if {@link ValueResolverContext#isCollectProperty()}
     * returns true, using {@link ValueResolver#getSupportedProperties()}.
     * Only collects methods if {@link ValueResolverContext#isCollectMethod()}
     * returns true, using {@link ValueResolver#getSupportedMethods()}.
     *
     * @param valueResolver the value resolver to collect from
     * @param context the context to fill with properties and methods
     */
    @Override
    public void collect(ValueResolver valueResolver, ValueResolverContext context) {
        if (context.isCollectProperty()) {
            valueResolver.getSupportedProperties().forEach(property -> context.addProperty(property));
        }
        if (context.isCollectMethod()) {
            valueResolver.getSupportedMethods().forEach(method -> context.addMethod(method));
        }
    }

}
