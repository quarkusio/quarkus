package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.smallrye.config.SmallRyeConfig;

public class ConfigPropertiesCreator implements BeanCreator<Object> {
    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        InjectionPoint injectionPoint = context.getInjectedReference(InjectionPoint.class);
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }

        Class<?> interfaceType = (Class<?>) context.getParams().get("type");
        String prefix = (String) context.getParams().get("prefix");

        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        return config.getConfigMapping(interfaceType, getPrefixFromInjectionPoint(injectionPoint, prefix));
    }

    private static String getPrefixFromInjectionPoint(final InjectionPoint injectionPoint, final String prefix) {
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated != null) {
            ConfigProperties configProperties = annotated.getAnnotation(ConfigProperties.class);
            if (configProperties != null) {
                if (!ConfigProperties.UNCONFIGURED_PREFIX.equals(configProperties.prefix())) {
                    return configProperties.prefix();
                }
            }
        }

        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier instanceof ConfigProperties configPropertiesQualifier) {
                if (!ConfigProperties.UNCONFIGURED_PREFIX.equals(configPropertiesQualifier.prefix())) {
                    return configPropertiesQualifier.prefix();
                }
            }
        }

        return prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX) ? "" : prefix;
    }
}
