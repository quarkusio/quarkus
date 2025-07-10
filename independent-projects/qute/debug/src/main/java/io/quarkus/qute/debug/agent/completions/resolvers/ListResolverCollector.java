package io.quarkus.qute.debug.agent.completions.resolvers;

import java.util.List;

public class ListResolverCollector extends SimpleResolverCollector {

    public ListResolverCollector() {
        super(List.of("get(${index})", "take", "takeLast", "first", "last", "${index}"));
    }

    @Override
    public String getClassName() {
        return "io.quarkus.qute.ValueResolvers$ListResolver";
    }

}
