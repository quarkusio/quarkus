package io.quarkus.resteasy.jackson.deployment;

import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.deployment.ResteasyJsonConfig;
import io.quarkus.resteasy.jackson.runtime.QuarkusJacksonSerializer;

public class ResteasyJacksonProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_JACKSON));
    }

    @BuildStep
    void capabilities(BuildProducer<CapabilityBuildItem> capability) {
        capability.produce(new CapabilityBuildItem(Capability.RESTEASY_JSON));
        capability.produce(new CapabilityBuildItem(Capability.REST_JACKSON));
    }

    @BuildStep
    ResteasyJaxrsProviderBuildItem provider(ResteasyJsonConfig config) {
        if (config.jsonDefault) {
            return new ResteasyJaxrsProviderBuildItem(QuarkusJacksonSerializer.class.getName());
        }
        return null;
    }
}
