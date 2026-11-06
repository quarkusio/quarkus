package io.quarkus.undertow.deployment;

import org.jboss.metadata.property.SimpleExpressionResolver;

import io.smallrye.config.Config;

public class MPConfigExpressionResolver implements SimpleExpressionResolver {

    @Override
    public ResolutionResult resolveExpressionContent(String expressionContent) {
        String value = Config.get().getOptionalValue(expressionContent, String.class).orElse(null);
        return (value == null) ? null : new ResolutionResult(value, false);
    }
}