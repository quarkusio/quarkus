package org.jboss.shamrock.deployment;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;


public class ConfigInjectionProvider implements InjectionProvider {

    @Override
    public Set<Class<?>> getProvidedTypes() {
        return Collections.singleton(ShamrockConfig.class);
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return (T) ShamrockConfig.INSTANCE;
    }
}
