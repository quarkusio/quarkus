package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.graal.DisableLoggingFeature;

// Note this is necessary even if Hibernate Search is disabled
class HibernateSearchElasticsearchLoggingProcessor {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(DisableLoggingFeature.class);
    }

    // Note this is necessary even if Hibernate Search is disabled
    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        // if the category changes, please also update DisableLoggingFeature in the runtime module
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.search.mapper.orm.bootstrap.impl.HibernateSearchPreIntegrationService", "HSEARCH000034"));
    }

}
