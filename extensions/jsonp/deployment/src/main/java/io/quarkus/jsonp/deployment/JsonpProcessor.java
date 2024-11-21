package io.quarkus.jsonp.deployment;

import jakarta.json.spi.JsonProvider;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class JsonpProcessor {

    @BuildStep
    void build(BuildProducer<ServiceProviderBuildItem> serviceProviders) {

        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(JsonProvider.class.getName()));
    }

}
