package io.quarkus.optaplanner.jsonb.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.optaplanner.jsonb.OptaPlannerJsonbConfigCustomizer;

class OptaPlannerJsonbProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPTAPLANNER_JSONB);
    }

    @BuildStep
    void registerOptaPlannerJsonbConfig(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(OptaPlannerJsonbConfigCustomizer.class));
    }

}
