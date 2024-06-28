package io.quarkus.qute.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.qute.runtime.jsonobject.JsonObjectValueResolver;

public class JsonObjectProcessor {

    @BuildStep
    void init(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (capabilities.isPresent(Capability.VERTX)) {
            beans.produce(new AdditionalBeanBuildItem(JsonObjectValueResolver.class));
        }
    }
}
