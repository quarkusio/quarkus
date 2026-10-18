package io.quarkus.proxy.deployment;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.proxy.runtime.ProxyConfigurationRecorder;
import io.quarkus.proxy.runtime.ProxyConfigurationRegistryImpl;
import io.quarkus.proxy.runtime.config.ProxyConfig;

public class ProxyRegistryProcessor {
    @BuildStep
    void initializeProxyConfigurationRegistry(
            ServiceRegistrar reg,
            BuildProducer<ProxyRegistryBuildItem> registry,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        reg
                .forService(ProxyConfigurationRegistry.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(ProxyConfig.class)
                .onStart((ctx, config) -> ProxyConfigurationRecorder.createRegistry(config));

        // temporary bridge: supplier-based build item for unconverted recorder consumers
        registry.produce(new ProxyRegistryBuildItem(
                reg.serviceAsRecorderSupplier(ProxyConfigurationRegistry.class)));

        syntheticBeans.produce(SyntheticBeanBuildItem.create(ProxyConfigurationRegistryImpl.class)
                .addType(ProxyConfigurationRegistry.class)
                .runtimeValue(reg.serviceAsRuntimeValue(ProxyConfigurationRegistry.class))
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .done());
    }
}
