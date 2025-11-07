package io.quarkus.proxy.deployment;

import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.proxy.runtime.ProxyConfigurationRecorder;
import io.quarkus.proxy.runtime.ProxyConfigurationRegistryImpl;

public class ProxyRegistryProcessor {
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeProxyConfigurationRegistry(
            ProxyConfigurationRecorder recorder,
            BuildProducer<ProxyRegistryBuildItem> registry,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        Supplier<ProxyConfigurationRegistry> supplier = recorder.init();

        registry.produce(new ProxyRegistryBuildItem(supplier));

        syntheticBeans.produce(SyntheticBeanBuildItem.create(ProxyConfigurationRegistryImpl.class)
                .addType(ProxyConfigurationRegistry.class)
                .supplier(supplier)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .done());

    }
}
