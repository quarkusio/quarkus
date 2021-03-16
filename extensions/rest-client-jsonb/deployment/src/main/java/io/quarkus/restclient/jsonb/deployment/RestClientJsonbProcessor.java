package io.quarkus.restclient.jsonb.deployment;

import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class RestClientJsonbProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem(Feature.REST_CLIENT_JSONB));

        capability.produce(new CapabilityBuildItem(Capability.REST_JSONB));
        capability.produce(new CapabilityBuildItem(Capability.RESTEASY_JSON));
    }
}
