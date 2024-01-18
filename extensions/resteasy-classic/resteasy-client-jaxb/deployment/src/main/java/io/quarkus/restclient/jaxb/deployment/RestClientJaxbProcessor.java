package io.quarkus.restclient.jaxb.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class RestClientJaxbProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_CLIENT_JAXB));
    }
}
