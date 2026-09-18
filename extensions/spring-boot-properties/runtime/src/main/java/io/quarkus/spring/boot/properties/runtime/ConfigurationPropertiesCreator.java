package io.quarkus.spring.boot.properties.runtime;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.smallrye.config.Config;

public class ConfigurationPropertiesCreator implements BeanCreator<Object> {
    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        InjectionPoint injectionPoint = context.getInjectedReference(InjectionPoint.class);
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }

        Class<?> interfaceType = (Class<?>) context.getParams().get("type");
        String prefix = (String) context.getParams().get("prefix");

        Config config = Config.get();
        return config.getConfigMapping(interfaceType, getPrefixFromInjectionPoint(injectionPoint, prefix));
    }

    private static String getPrefixFromInjectionPoint(final InjectionPoint injectionPoint, final String prefix) {
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated != null) {
            ConfigurationProperties configurationProperties = annotated.getAnnotation(ConfigurationProperties.class);
            if (configurationProperties != null) {
                if (!configurationProperties.prefix().isEmpty()) {
                    return configurationProperties.prefix();
                }
            }
        }
        return prefix;
    }
}
