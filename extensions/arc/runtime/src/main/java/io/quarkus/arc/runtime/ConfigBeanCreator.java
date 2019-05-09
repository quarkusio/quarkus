package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InjectionPointProvider;
import io.smallrye.config.SmallRyeConfig;

public class ConfigBeanCreator implements BeanCreator<Object> {

    @Override
    public Object create(CreationalContext<Object> creationalContext, Map<String, Object> params) {
        String requiredType = params.get("requiredType").toString();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ConfigBeanCreator.class.getClassLoader();
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(requiredType, true, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load required type: " + requiredType);
        }

        InjectionPoint injectionPoint = InjectionPointProvider.get();
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }

        ConfigProperty configProperty = getConfigProperty(injectionPoint);
        if (configProperty == null) {
            throw new IllegalStateException("@ConfigProperty not found");
        }

        String key = configProperty.name();
        String defaultValue = configProperty.defaultValue();

        if (defaultValue.isEmpty() || ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue)) {
            return getConfig().getValue(key, clazz);
        } else {
            Config config = getConfig();
            Optional<?> value = config.getOptionalValue(key, clazz);
            if (value.isPresent()) {
                return value.get();
            } else {
                return ((SmallRyeConfig) config).convert(defaultValue, clazz);
            }
        }
    }

    private Config getConfig() {
        return ConfigProviderResolver.instance().getConfig();
    }

    private ConfigProperty getConfigProperty(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                return (ConfigProperty) qualifier;
            }
        }
        return null;
    }

}
