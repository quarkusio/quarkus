package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Optional;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;

public class ConfigMappingCreator implements BeanCreator<Object> {

    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        InjectionPoint injectionPoint = context.getInjectedReference(InjectionPoint.class);
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }

        Class<?> interfaceType = (Class<?>) context.getParams().get("type");
        String prefix = (String) context.getParams().get("prefix");

        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        return config.getConfigMapping(interfaceType, getPrefixFromInjectionPoint(injectionPoint).orElse(prefix));
    }

    private static Optional<String> getPrefixFromInjectionPoint(final InjectionPoint injectionPoint) {
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated != null) {
            ConfigMapping configMapping = annotated.getAnnotation(ConfigMapping.class);
            if (configMapping != null) {
                if (!configMapping.prefix().isEmpty()) {
                    return Optional.of(configMapping.prefix());
                }
            }

            ConfigProperties configProperties = annotated.getAnnotation(ConfigProperties.class);
            if (configProperties != null) {
                if (!ConfigProperties.UNCONFIGURED_PREFIX.equals(configProperties.prefix())) {
                    return Optional.of(configProperties.prefix());
                }
            }
        }

        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier instanceof ConfigProperties) {
                ConfigProperties configPropertiesQualifier = (ConfigProperties) qualifier;
                if (!ConfigProperties.UNCONFIGURED_PREFIX.equals(configPropertiesQualifier.prefix())) {
                    return Optional.of(configPropertiesQualifier.prefix());
                }
            }
        }

        return Optional.empty();
    }
}
