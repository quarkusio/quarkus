package io.quarkus.elasticsearch.highlevel.client.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elasticsearch.highlevel.client.ElasticsearchRestHighLevelClientProducer;

class ElasticsearchHighLevelClientProcessor {

    private static final String FEATURE = "elasticsearch-rest-high-level-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep()
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(ElasticsearchRestHighLevelClientProducer.class);
    }

}
