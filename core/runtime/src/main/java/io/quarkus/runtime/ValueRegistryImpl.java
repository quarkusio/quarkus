package io.quarkus.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.registry.RuntimeInfoProvider;
import io.quarkus.registry.RuntimeInfoProvider.RuntimeSource;
import io.quarkus.registry.ValueRegistry;
import io.quarkus.registry.ValueRegistry.RuntimeInfo.SimpleRuntimeInfo;
import io.smallrye.config.SmallRyeConfig;

/**
 * Implementation of {@link ValueRegistry}.
 * <p>
 * Each Quarkus application has its own separate instance, created on application start. The {@link ValueRegistry} is
 * then stored in the {@link io.quarkus.runtime.StartupContext} before any recorders are executed, so that it can be
 * injected into the recorders' constructors as a {@link RuntimeValue}.
 *
 * @see Application#Application(boolean)
 * @see "io.quarkus.deployment.steps.MainClassBuildStep#build for storage"
 * @see "io.quarkus.deployment.ExtensionLoader#loadStepsFrom for retrieval"
 * @see "io.quarkus.deployment.recording.ObjectLoader"
 */
public class ValueRegistryImpl implements ValueRegistry {
    private final Map<String, RuntimeInfo<?>> values = new ConcurrentHashMap<>();

    private ValueRegistryImpl() {
    }

    public <T> void register(final RuntimeKey<T> key, final T value) {
        if (key == null || key.key() == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        registerInfo(key, SimpleRuntimeInfo.of(value));
    }

    public <T> void registerInfo(final RuntimeKey<T> key, final RuntimeInfo<T> runtimeInfo) {
        if (key == null || key.key() == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (runtimeInfo == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        RuntimeInfo<?> mapValue = values.putIfAbsent(key.key(), runtimeInfo);
        if (mapValue != null) {
            throw new IllegalArgumentException("Key already registered " + key.key());
        }
    }

    public <T> boolean containsKey(final RuntimeKey<T> key) {
        return values.containsKey(key.key());
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final RuntimeKey<T> key) {
        RuntimeInfo<T> runtimeInfo = (RuntimeInfo<T>) values.get(key.key());
        if (runtimeInfo == null) {
            throw new IllegalArgumentException("Key " + key.key() + " not found");
        }
        return runtimeInfo.get(this);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(final RuntimeKey<T> key, final T defaultValue) {
        RuntimeInfo<T> runtimeInfo = (RuntimeInfo<T>) values.get(key.key());
        return runtimeInfo == null ? defaultValue : runtimeInfo.get(this);
    }

    @Override
    public RuntimeInfo<?> get(final String key) {
        return values.get(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean discoverInfos;
        private final List<RuntimeSource> sources = new ArrayList<>();

        private Builder() {
        }

        public Builder addDiscoveredInfos() {
            this.discoverInfos = true;
            return this;
        }

        public Builder withRuntimeSource(final SmallRyeConfig config) {
            this.sources.add(new ConfigRuntimeSource(config));
            return this;
        }

        public ValueRegistry build() {
            ValueRegistryImpl valueRegistry = new ValueRegistryImpl();
            if (discoverInfos) {
                ServiceLoader<RuntimeInfoProvider> infoProviders = ServiceLoader.load(RuntimeInfoProvider.class);
                for (RuntimeInfoProvider runtimeInfoProvider : infoProviders) {
                    runtimeInfoProvider.register(valueRegistry, new RuntimeSource() {
                        @Override
                        public <T> T get(RuntimeKey<T> key) {
                            T value;
                            for (RuntimeSource source : sources) {
                                value = source.get(key);
                                if (value != null) {
                                    return value;
                                }
                            }
                            return null;
                        }
                    });
                }
            }
            return valueRegistry;
        }
    }

    public static class ConfigRuntimeSource implements RuntimeSource {
        private final Config config;

        ConfigRuntimeSource(Config config) {
            this.config = config;
        }

        @Override
        public <T> T get(RuntimeKey<T> key) {
            return config.getOptionalValue(key.key(), key.type()).orElse(null);
        }

        public static RuntimeSource runtimeSource(Config config) {
            return new ConfigRuntimeSource(config);
        }

        public static RuntimeSource runtimeSource() {
            return new ConfigRuntimeSource(ConfigProvider.getConfig());
        }
    }
}
