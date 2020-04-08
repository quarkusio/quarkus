package io.quarkus.elasticsearch.highlevel.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.elasticsearch.highlevel.client.ElasticsearchRestHighLevelClientProducer;
import io.quarkus.elasticsearch.highlevel.client.ElasticsearchRestHighLevelClientRecorder;
import io.quarkus.elasticsearch.lowlevel.client.ElasticsearchConfig;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class ElasticsearchHighLevelClientProcessor {

    private static final String FEATURE = "elasticsearch-high-level-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep()
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(ElasticsearchRestHighLevelClientProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureRestClient(ElasticsearchRestHighLevelClientRecorder restClientRecorder,
            BeanContainerBuildItem beanContainerBuildItem, ShutdownContextBuildItem shutdownContext,
            ElasticsearchConfig configuration) {

        restClientRecorder.configureRestClient(beanContainerBuildItem.getValue(), configuration, shutdownContext);
    }

    @BuildStep
    HealthBuildItem addHealthCheck(ElasticsearchBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.elasticsearch.restclient.health.ElasticsearchHealthCheck",
                buildTimeConfig.healthEnabled, "elasticsearch");
    }

}
