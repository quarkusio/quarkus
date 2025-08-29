package io.quarkus.qute.debug.agent.resolvers;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;

public interface ValueResolverCollector {

    boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext);

    void collect(ValueResolver valueResolver, ValueResolverContext context);
}
