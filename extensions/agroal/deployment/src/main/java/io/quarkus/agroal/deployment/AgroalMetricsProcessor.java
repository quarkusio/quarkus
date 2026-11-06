package io.quarkus.agroal.deployment;

import java.util.List;

import io.quarkus.agroal.runtime.metrics.AgroalMetricsConsumer;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;

public class AgroalMetricsProcessor {

    @BuildStep
    void registerMetrics(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            BuildProducer<MetricsFactoryConsumerBuildItem> datasourceMetrics,
            List<JdbcDataSourceDefinitionBuildItem> aggregatedDataSourceBuildTimeConfigs) {

        for (JdbcDataSourceDefinitionBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedDataSourceBuildTimeConfigs) {
            // Create a MetricsFactory consumer to register metrics for a data source
            // IFF metrics are enabled globally and for the data source
            // (they are enabled for each data source by default if they are also enabled globally)
            if (dataSourcesBuildTimeConfig.metricsEnabled() &&
                    aggregatedDataSourceBuildTimeConfig.getJdbcConfig().metrics().enabled().orElse(true)) {
                datasourceMetrics.produce(new MetricsFactoryConsumerBuildItem(
                        new AgroalMetricsConsumer(aggregatedDataSourceBuildTimeConfig.getName())));
            }
        }
    }
}
