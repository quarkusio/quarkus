package io.quarkus.elasticsearch.restclient.highlevel.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elasticsearch.restclient.highlevel.runtime.ElasticsearchRestHighLevelClientProducer;

class ElasticsearchHighLevelClientProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.ELASTICSEARCH_REST_HIGH_LEVEL_CLIENT);
    }

    @BuildStep()
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(ElasticsearchRestHighLevelClientProducer.class);
    }

}
