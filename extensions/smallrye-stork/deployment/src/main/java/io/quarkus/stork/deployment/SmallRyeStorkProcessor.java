package io.quarkus.stork.deployment;

import static java.util.Arrays.asList;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.stork.SmallRyeStorkRecorder;
import io.smallrye.stork.microprofile.MicroProfileConfigProvider;
import io.smallrye.stork.spi.LoadBalancerProvider;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;

public class SmallRyeStorkProcessor {

    @BuildStep
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> services) {
        services.produce(new ServiceProviderBuildItem(io.smallrye.stork.config.ConfigProvider.class.getName(),
                MicroProfileConfigProvider.class.getName()));

        for (Class<?> providerClass : asList(LoadBalancerProvider.class, ServiceDiscoveryProvider.class)) {
            services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(providerClass.getName()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    void initializeStork(SmallRyeStorkRecorder storkRecorder, ShutdownContextBuildItem shutdown) {
        storkRecorder.initialize(shutdown);
    }
}
