package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class InetAddressProcessor {

    @BuildStep
    void registerInetAddressServiceProvider(BuildProducer<ServiceProviderBuildItem> services) {
        // service provider loaded by java.net.InetAddress.loadResolver
        services.produce(ServiceProviderBuildItem.allProvidersFromClassPath("java.net.spi.InetAddressResolverProvider"));
    }

}
