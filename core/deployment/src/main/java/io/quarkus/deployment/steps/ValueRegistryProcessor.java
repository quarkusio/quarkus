package io.quarkus.deployment.steps;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.registry.RuntimeInfoProvider;

class ValueRegistryProcessor {
    @BuildStep
    void serviceProvider(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(ServiceProviderBuildItem.allProvidersFromClassPath(RuntimeInfoProvider.class.getName()));
    }
}
