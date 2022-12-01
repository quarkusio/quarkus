package io.quarkus.rest.client.reactive.jsonb.deployment;

import static io.quarkus.deployment.Feature.REST_CLIENT_REACTIVE_JSONB;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class RestClientReactiveJsonbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(REST_CLIENT_REACTIVE_JSONB));
    }
}
