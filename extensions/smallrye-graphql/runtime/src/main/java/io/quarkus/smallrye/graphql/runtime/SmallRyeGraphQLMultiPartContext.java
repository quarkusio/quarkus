package io.quarkus.smallrye.graphql.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.smallrye.graphql.execution.context.SmallRyeContext;

public class SmallRyeGraphQLMultiPartContext extends SmallRyeContext {

    private final Map<String, Object> multiPartVariables;

    public SmallRyeGraphQLMultiPartContext(Map<String, Object> multiPartVariables) {
        super(SmallRyeGraphQLMultiPartContext.class.getName());
        this.multiPartVariables = multiPartVariables;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getVariables() {
        return Optional.of(super.getVariables()
                .map(HashMap::new)
                .map(m -> {
                    m.putAll(multiPartVariables);
                    return m;
                }).map(Map.class::cast)
                .orElse(multiPartVariables));
    }
}
