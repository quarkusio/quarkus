package io.quarkus.restclient.jackson.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class RestClientJacksonProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.REST_CLIENT_JACKSON));
    }
}
