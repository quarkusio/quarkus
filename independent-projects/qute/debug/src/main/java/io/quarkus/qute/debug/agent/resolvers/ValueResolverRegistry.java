package io.quarkus.qute.debug.agent.resolvers;

import io.quarkus.qute.ValueResolver;

/**
 * Registry for Qute {@link ValueResolver}s to support debugging completions
 * and variable inspection.
 * <p>
 * This class manages which collector to use for a given {@link ValueResolver}
 * and provides methods to fill a {@link ValueResolverContext} with available
 * properties and methods from Qute templates.
 * </p>
 */
public class ValueResolverRegistry {

    /** Fully qualified class name of the ReflectionValueResolver. */
    private static final String IO_QUARKUS_QUTE_REFLECTION_VALUE_RESOLVER = "io.quarkus.qute.ReflectionValueResolver";

    /** Singleton instance of the registry. */
    private static final ValueResolverRegistry INSTANCE = new ValueResolverRegistry();

    /** Collector specifically for ReflectionValueResolver instances. */
    private static final ValueResolverCollector REFLECTION_VALUE_RESOLVER_COLLECTOR = new ReflectionValueResolverCollector();

    /** Default collector used for all other ValueResolvers. */
    private static final ValueResolverCollector DEFAULT_VALUE_RESOLVER_COLLECTOR = new DefaultValueResolverCollector();

    private ValueResolverRegistry() {
        // private constructor for singleton
    }

    /**
     * Returns the singleton instance of the registry.
     *
     * @return the {@link ValueResolverRegistry} instance
     */
    public static ValueResolverRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Populates the given {@link ValueResolverContext} with available properties
     * and methods for the base object using all value resolvers registered in
     * the Qute engine.
     *
     * @param context the context to fill with properties/methods
     */
    public void fillWithValueResolvers(ValueResolverContext context) {
        var base = context.getBase();
        var stackFrame = context.getStackFrame();
        var engine = stackFrame.getEngine();
        var evalContext = stackFrame.createEvalContext(base);
        var valueResolvers = engine.getValueResolvers();

        for (ValueResolver valueResolver : valueResolvers) {
            var collector = getCollector(valueResolver);
            if (collector != null && collector.isApplicable(valueResolver, evalContext)) {
                collector.collect(valueResolver, context);
            }
        }
    }

    /**
     * Fills the context using only the default reflection-based value resolver.
     *
     * @param context the context to fill
     */
    public void fillWithReflectionValueResolver(ValueResolverContext context) {
        DEFAULT_VALUE_RESOLVER_COLLECTOR.collect(null, context);
    }

    /**
     * Returns the appropriate collector for a given {@link ValueResolver}.
     *
     * @param valueResolver the resolver to check
     * @return the matching {@link ValueResolverCollector}
     */
    private ValueResolverCollector getCollector(ValueResolver valueResolver) {
        if (isReflectionValueResolver(valueResolver)) {
            return REFLECTION_VALUE_RESOLVER_COLLECTOR;
        }
        return DEFAULT_VALUE_RESOLVER_COLLECTOR;
    }

    /**
     * Checks if the given {@link ValueResolver} is the built-in
     * ReflectionValueResolver.
     *
     * @param valueResolver the resolver to check
     * @return true if it's a ReflectionValueResolver, false otherwise
     */
    public static boolean isReflectionValueResolver(ValueResolver valueResolver) {
        String className = valueResolver.getClass().getName();
        return IO_QUARKUS_QUTE_REFLECTION_VALUE_RESOLVER.equals(className);
    }
}
