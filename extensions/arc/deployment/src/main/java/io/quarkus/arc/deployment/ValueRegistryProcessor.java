package io.quarkus.arc.deployment;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import io.quarkus.arc.runtime.ValueRegistryRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.value.registry.RuntimeInfoProvider;
import io.quarkus.value.registry.RuntimeInfoProvider.RuntimeSource;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;

class ValueRegistryProcessor {
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void valueRegistry(
            ValueRegistryRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(ValueRegistry.class)
                .startup()
                .setRuntimeInit()
                .unremovable()
                .supplier(recorder.valueRegistry())
                .scope(ApplicationScoped.class)
                .addQualifier(Default.class);

        syntheticBeans.produce(configurator.done());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void runtimeInfo(
            ValueRegistryRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        for (Class<?> runtimeInfo : getRuntimeInfoClasses()) {
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(runtimeInfo)
                    .startup()
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.runtimeInfo(runtimeInfo))
                    .scope(ApplicationScoped.class)
                    .addQualifier(Default.class);

            syntheticBeans.produce(configurator.done());
        }
    }

    private static Set<Class<?>> getRuntimeInfoClasses() {
        Set<Class<?>> runtimeInfos = new HashSet<>();
        ValueRegistry valueRegistry = new ValueRegistry() {
            @Override
            public <T> void register(RuntimeKey<T> key, T value) {
            }

            @Override
            public <T> void registerInfo(RuntimeKey<T> key, RuntimeInfo<T> runtimeInfo) {
                try {
                    Class<?> klass = Thread.currentThread().getContextClassLoader().loadClass(key.key());
                    runtimeInfos.add(klass);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }

            @Override
            public <T> T get(RuntimeKey<T> key) {
                return null;
            }

            @Override
            public <T> T getOrDefault(RuntimeKey<T> key, T defaultValue) {
                return null;
            }

            @Override
            public <T> boolean containsKey(RuntimeKey<T> key) {
                return false;
            }

            @Override
            public RuntimeInfo<?> get(String key) {
                return null;
            }
        };

        // To collect RuntimeInfo implementations and register them as CDI Beans
        ServiceLoader<RuntimeInfoProvider> infoProviders = ServiceLoader.load(RuntimeInfoProvider.class);
        for (RuntimeInfoProvider runtimeInfoProvider : infoProviders) {
            runtimeInfoProvider.register(valueRegistry, new RuntimeSource() {
                @Override
                public <T> T get(RuntimeKey<T> key) {
                    return null;
                }
            });
        }
        return runtimeInfos;
    }
}
