package io.quarkus.qute.debug.agent.resolvers;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;

public class DefaultValueResolverCollector implements ValueResolverCollector {

    @Override
    public boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext) {
        return valueResolver.appliesTo(evalContext);
    }

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
