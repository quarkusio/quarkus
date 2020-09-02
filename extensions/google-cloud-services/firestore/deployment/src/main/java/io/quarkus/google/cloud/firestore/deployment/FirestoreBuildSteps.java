package io.quarkus.google.cloud.firestore.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.google.cloud.firestore.runtime.FirestoreProducer;

public class FirestoreBuildSteps {
    private static final String FEATURE = "google-cloud-firestore";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(FirestoreProducer.class);
    }
}
