package io.quarkus.resteasy.multipart.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.multipart.runtime.MultipartInputPartConfigContainerRequestFilter;

public class ResteasyMultipartProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.RESTEASY_MULTIPART));
    }

    @BuildStep
    AdditionalBeanBuildItem filter() {
        return new AdditionalBeanBuildItem.Builder()
                .addBeanClass(MultipartInputPartConfigContainerRequestFilter.class)
                .setUnremovable()
                .setDefaultScope(DotNames.SINGLETON)
                .build();
    }
}
