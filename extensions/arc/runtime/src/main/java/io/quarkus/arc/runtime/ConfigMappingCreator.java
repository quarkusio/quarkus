package io.quarkus.arc.runtime;

import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.BeanCreator;
import io.smallrye.config.SmallRyeConfig;

public class ConfigMappingCreator implements BeanCreator<Object> {
    @Override
    public Object create(CreationalContext<Object> creationalContext, Map<String, Object> params) {
        Class<?> interfaceType = (Class<?>) params.get("type");
        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        return config.getConfigMapping(interfaceType);
    }
}
