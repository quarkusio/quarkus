package io.quarkus.stork.deployment;

import static java.util.Arrays.asList;

import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.stork.SmallRyeStorkRecorder;
import io.quarkus.stork.StorkConfigProvider;
import io.quarkus.stork.StorkConfiguration;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.smallrye.stork.spi.internal.LoadBalancerLoader;
import io.smallrye.stork.spi.internal.ServiceDiscoveryLoader;

public class SmallRyeStorkProcessor {

    @BuildStep
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> services) {
        services.produce(new ServiceProviderBuildItem(io.smallrye.stork.spi.config.ConfigProvider.class.getName(),
                StorkConfigProvider.class.getName()));

        for (Class<?> providerClass : asList(LoadBalancerLoader.class, ServiceDiscoveryLoader.class)) {
            services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(providerClass.getName()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void initializeStork(SmallRyeStorkRecorder storkRecorder, ShutdownContextBuildItem shutdown, VertxBuildItem vertx,
            StorkConfiguration configuration) {
        storkRecorder.initialize(shutdown, vertx.getVertx(), configuration);
    }

}
