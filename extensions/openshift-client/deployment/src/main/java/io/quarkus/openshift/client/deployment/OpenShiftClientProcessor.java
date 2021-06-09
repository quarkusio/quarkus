package io.quarkus.openshift.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.it.openshift.client.runtime.OpenShiftClientProducer;

public class OpenShiftClientProcessor {

    @BuildStep
    public void process(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemProducer) {
        // wire up the OpenShiftClient bean support
        additionalBeanBuildItemProducer.produce(AdditionalBeanBuildItem.unremovableOf(OpenShiftClientProducer.class));
    }

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPENSHIFT_CLIENT);
    }
}
