package io.quarkus.arc.runtime;

import static io.smallrye.config.inject.ConfigMappingInjectionBean.getPrefixFromInjectionPoint;

import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.smallrye.config.SmallRyeConfig;

public class ConfigMappingCreator implements BeanCreator<Object> {
    @Override
    public Object create(CreationalContext<Object> creationalContext, Map<String, Object> params) {
        InjectionPoint injectionPoint = InjectionPointProvider.get();
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }

        Class<?> interfaceType = (Class<?>) params.get("type");
        String prefix = (String) params.get("prefix");

        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        return config.getConfigMapping(interfaceType, getPrefixFromInjectionPoint(injectionPoint).orElse(prefix));
    }
}
