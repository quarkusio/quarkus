package io.quarkus.elasticsearch.lowlevel.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.elasticsearch.lowlevel.client.ElasticsearchConfig;
import io.quarkus.elasticsearch.lowlevel.client.ElasticsearchRestClientProducer;
import io.quarkus.elasticsearch.lowlevel.client.ElasticsearchRestClientRecorder;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class ElasticsearchLowLevelClientProcessor {

    private static final String FEATURE = "elasticsearch-low-level-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep()
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(ElasticsearchRestClientProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureRestClient(ElasticsearchRestClientRecorder restClientRecorder,
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
