package io.quarkus.resteasy.reactive.problem.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class QuarkusResteasyReactiveProblemProcessor {

    private static final String FEATURE = "quarkus-resteasy-reactive-problem";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
