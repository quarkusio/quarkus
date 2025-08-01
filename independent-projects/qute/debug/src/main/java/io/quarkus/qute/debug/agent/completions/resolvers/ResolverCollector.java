package io.quarkus.qute.debug.agent.completions.resolvers;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.debug.agent.completions.CompletionContext;

public interface ResolverCollector {

    String getClassName();

    default boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext) {
        return valueResolver.appliesTo(evalContext);
    }

    void collect(CompletionContext context);
}
