package io.quarkus.runtime.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Config source that is used to handle {@code io.quarkus.bootstrap.app.StartupAction#overrideConfig(java.util.Map)}
 *
 */
public class RuntimeOverrideConfigSource implements ConfigSource {

    public static final String FIELD_NAME = "CONFIG";
    public static final String GENERATED_CLASS_NAME = RuntimeOverrideConfigSource.class.getName() + "$$GeneratedMapHolder";

    final Map<String, String> values;

    public RuntimeOverrideConfigSource(ClassLoader classLoader) {
        try {
            Class<?> cls = classLoader.loadClass(GENERATED_CLASS_NAME);
            Map<String, String> values = (Map<String, String>) cls.getDeclaredField(FIELD_NAME).get(null);
            this.values = values == null ? Collections.emptyMap() : values;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void setConfig(ClassLoader runtimeClassLoader, Map<String, String> config) {
        try {
            Class<?> cls = runtimeClassLoader.loadClass(GENERATED_CLASS_NAME);
            cls.getDeclaredField(FIELD_NAME).set(null, new HashMap<>(config));
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>(values);
    }

    @Override
    public Set<String> getPropertyNames() {
        return values.keySet();
    }

    @Override
    public int getOrdinal() {
        return 399; //one less that system properties
    }

    @Override
    public String getValue(String s) {
        return values.get(s);
    }

    @Override
    public String getName() {
        return "Config Override Config Source";
    }

}
