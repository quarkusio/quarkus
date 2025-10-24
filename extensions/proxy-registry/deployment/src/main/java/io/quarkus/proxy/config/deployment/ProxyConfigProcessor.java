package io.quarkus.proxy.config.deployment;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.proxy.config.ProxyConfig;
import io.quarkus.proxy.config.ProxyConfigurationRegistry;

public class ProxyConfigProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeProxyConfigurationRegistry(
            ProxyConfigurationRegistry recorder,
            ProxyConfig proxyConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        recorder.init(proxyConfig);
        syntheticBeans.produce(SyntheticBeanBuildItem
                .configure(ProxyConfigurationRegistry.class)
                .supplier(recorder.getSupplier())
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .done());

    }
}
