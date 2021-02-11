package io.quarkus.elasticsearch.restclient.lowlevel.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elasticsearch.restclient.lowlevel.runtime.ElasticsearchRestClientProducer;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class ElasticsearchLowLevelClientProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.ELASTICSEARCH_REST_CLIENT);
    }

    @BuildStep()
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(ElasticsearchRestClientProducer.class);
    }

    @BuildStep
    HealthBuildItem addHealthCheck(ElasticsearchBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.elasticsearch.restclient.lowlevel.runtime.health.ElasticsearchHealthCheck",
                buildTimeConfig.healthEnabled);
    }

}
