package io.quarkus.restclient.jackson.deployment;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class RestClientJacksonProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.REST_CLIENT_JACKSON));

        capability.produce(new CapabilityBuildItem(Capabilities.REST_JACKSON));
    }
}
