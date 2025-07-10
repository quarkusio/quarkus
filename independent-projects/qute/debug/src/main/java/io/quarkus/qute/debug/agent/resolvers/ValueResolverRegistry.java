package io.quarkus.qute.debug.agent.resolvers;

import io.quarkus.qute.ValueResolver;

public class ValueResolverRegistry {

    private static final String IO_QUARKUS_QUTE_REFLECTION_VALUE_RESOLVER = "io.quarkus.qute.ReflectionValueResolver";

    private static final ValueResolverRegistry INSTANCE = new ValueResolverRegistry();

    private static final ValueResolverCollector REFLECTION_VALUE_RESOLVER_COLLECTOR = new ReflectionValueResolverCollector();
    private static final ValueResolverCollector DEFAULT_VALUE_RESOLVER_COLLECTOR = new DefaultValueResolverCollector();

    public static ValueResolverRegistry getInstance() {
        return INSTANCE;
    }

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

    public void fillWithReflectionValueResolver(ValueResolverContext context) {
        DEFAULT_VALUE_RESOLVER_COLLECTOR.collect(null, context);
    }

    private ValueResolverCollector getCollector(ValueResolver valueResolver) {
        if (isReflectionValueResolver(valueResolver)) {
            return REFLECTION_VALUE_RESOLVER_COLLECTOR;
        }
        return DEFAULT_VALUE_RESOLVER_COLLECTOR;
    }

    public static boolean isReflectionValueResolver(ValueResolver valueResolver) {
        String className = valueResolver.getClass().getName();
        return IO_QUARKUS_QUTE_REFLECTION_VALUE_RESOLVER.equals(className);
    }

}
