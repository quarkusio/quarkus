package io.quarkus.google.cloud.spanner.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.google.coud.service.spanner.runtime.SpannerProducer;

public class SpannerBuildSteps {
    private static final String FEATURE = "google-cloud-spanner";

    @BuildStep
    public ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, true, "com.google.protobuf.Empty");
    }

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem producer() {
        return new AdditionalBeanBuildItem(SpannerProducer.class);
    }
}
