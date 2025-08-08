package io.quarkus.qute.debug.agent.completions.resolvers;

import java.util.List;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ValueResolver;

public class ThisResolverCollector extends SimpleResolverCollector {

    public ThisResolverCollector() {
        super(List.of("this"));
    }

    @Override
    public boolean isApplicable(ValueResolver valueResolver, EvalContext evalContext) {
        return true;
    }

    @Override
    public String getClassName() {
        return "io.quarkus.qute.ValueResolvers$ThisResolver";
    }

}
