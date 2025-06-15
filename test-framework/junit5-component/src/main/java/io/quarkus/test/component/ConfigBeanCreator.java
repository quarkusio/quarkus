package io.quarkus.test.component;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class ConfigBeanCreator implements BeanCreator<Config> {

    // we need to keep a reference to the CL used to register the config object in order to support the continuous
    // testing where TCCL does not work for us
    private static final AtomicReference<ClassLoader> configClassLoader = new AtomicReference<>();

    @Override
    public Config create(SyntheticCreationalContext<Config> context) {
        return getConfig();
    }

    static Config getConfig() {
        return ConfigProvider.getConfig(configClassLoader.get());
    }

    static void setClassLoader(ClassLoader classLoader) {
        configClassLoader.set(classLoader);
    }

    static void clear() {
        configClassLoader.set(null);
    }

}
