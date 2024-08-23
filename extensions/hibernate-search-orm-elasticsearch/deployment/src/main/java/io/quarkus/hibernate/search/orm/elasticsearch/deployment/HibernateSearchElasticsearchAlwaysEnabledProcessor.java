package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

// Executed even if the extension is disabled, see https://github.com/quarkusio/quarkus/pull/26966/
class HibernateSearchElasticsearchAlwaysEnabledProcessor {

    @BuildStep
    public FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.HIBERNATE_SEARCH_ELASTICSEARCH);
    }

}
