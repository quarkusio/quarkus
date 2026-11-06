package io.quarkus.arc.runtime;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigMapping;

public class ConfigMappingCreator implements BeanCreator<Object> {
    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        InjectionPoint injectionPoint = context.getInjectedReference(InjectionPoint.class);
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }

        Class<?> interfaceType = (Class<?>) context.getParams().get("type");
        String prefix = (String) context.getParams().get("prefix");

        return Config.get().getConfigMapping(interfaceType, getPrefixFromInjectionPoint(injectionPoint, prefix));
    }

    private static String getPrefixFromInjectionPoint(final InjectionPoint injectionPoint, final String prefix) {
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated != null) {
            ConfigMapping configMapping = annotated.getAnnotation(ConfigMapping.class);
            if (configMapping != null) {
                if (!configMapping.prefix().isEmpty()) {
                    return configMapping.prefix();
                }
            }
        }
        return prefix;
    }
}
