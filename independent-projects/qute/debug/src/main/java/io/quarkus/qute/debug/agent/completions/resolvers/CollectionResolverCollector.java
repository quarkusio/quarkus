package io.quarkus.qute.debug.agent.completions.resolvers;

import java.util.List;

public class CollectionResolverCollector extends SimpleResolverCollector {

    public CollectionResolverCollector() {
        super(List.of("size", "isEmpty", "empty", "contains"));
    }

    @Override
    public String getClassName() {
        return "io.quarkus.qute.ValueResolvers$CollectionResolver";
    }

}
