package io.quarkus.elasticsearch.lowlevel.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elasticsearch.lowlevel.client.ElasticsearchRestClientProducer;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class ElasticsearchLowLevelClientProcessor {

    private static final String FEATURE = "elasticsearch-rest-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep()
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(ElasticsearchRestClientProducer.class);
    }

    @BuildStep
    HealthBuildItem addHealthCheck(ElasticsearchBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.elasticsearch.lowlevel.client.health.ElasticsearchHealthCheck",
                buildTimeConfig.healthEnabled, "elasticsearch");
    }

}
