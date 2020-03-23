package io.quarkus.restclient.jsonb.deployment;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class RestClientJsonbProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.REST_CLIENT_JSONB));

        capability.produce(new CapabilityBuildItem(Capabilities.REST_JSONB));
    }
}
