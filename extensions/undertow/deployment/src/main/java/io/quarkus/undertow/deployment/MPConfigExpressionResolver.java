package io.quarkus.undertow.deployment;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.metadata.property.SimpleExpressionResolver;

public class MPConfigExpressionResolver implements SimpleExpressionResolver {

    @Override
    public ResolutionResult resolveExpressionContent(String expressionContent) {
        String value = ConfigProvider.getConfig().getOptionalValue(expressionContent, String.class).orElse(null);
        return (value == null) ? null : new ResolutionResult(value, false);
    }
}
