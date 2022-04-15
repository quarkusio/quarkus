package io.quarkus.rest.client.reactive.jaxb.deployment;

import static io.quarkus.deployment.Feature.REST_CLIENT_REACTIVE_JAXB;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class RestClientReactiveJaxbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> features) {
        features.produce(new FeatureBuildItem(REST_CLIENT_REACTIVE_JAXB));
    }
}
