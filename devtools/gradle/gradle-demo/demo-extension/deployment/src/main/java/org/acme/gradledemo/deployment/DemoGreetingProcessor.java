package org.acme.gradledemo.deployment;

import org.acme.gradledemo.extension.DemoGreeting;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class DemoGreetingProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("gradle-demo-greeting");
    }

    @BuildStep
    AdditionalBeanBuildItem greeting() {
        return AdditionalBeanBuildItem.unremovableOf(DemoGreeting.class);
    }
}
