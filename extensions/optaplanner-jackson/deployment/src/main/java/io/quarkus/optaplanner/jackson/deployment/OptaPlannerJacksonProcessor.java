package io.quarkus.optaplanner.jackson.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.optaplanner.jackson.OptaPlannerObjectMapperCustomizer;

class OptaPlannerJacksonProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OPTAPLANNER_JACKSON);
    }

    @BuildStep
    void registerOptaPlannerJacksonModule(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(OptaPlannerObjectMapperCustomizer.class));
    }

}
