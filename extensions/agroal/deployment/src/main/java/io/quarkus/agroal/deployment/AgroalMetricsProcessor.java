package io.quarkus.agroal.deployment;

import java.util.List;

import io.quarkus.agroal.runtime.metrics.AgroalMetricsRecorder;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;

public class AgroalMetricsProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerMetrics(AgroalMetricsRecorder recorder,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            BuildProducer<MetricsFactoryConsumerBuildItem> datasourceMetrics,
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedDataSourceBuildTimeConfigs) {

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedDataSourceBuildTimeConfigs) {
            // Create a MetricsFactory consumer to register metrics for a data source
            // IFF metrics are enabled globally and for the data source
            // (they are enabled for each data source by default if they are also enabled globally)
            if (dataSourcesBuildTimeConfig.metricsEnabled &&
                    aggregatedDataSourceBuildTimeConfig.getJdbcConfig().enableMetrics.orElse(true)) {
                datasourceMetrics.produce(new MetricsFactoryConsumerBuildItem(
                        recorder.registerDataSourceMetrics(aggregatedDataSourceBuildTimeConfig.getName())));
            }
        }
    }
}
