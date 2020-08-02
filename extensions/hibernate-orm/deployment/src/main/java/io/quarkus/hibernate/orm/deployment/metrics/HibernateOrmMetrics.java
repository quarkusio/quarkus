package io.quarkus.hibernate.orm.deployment.metrics;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.runtime.metrics.HibernateMetricsRecorder;

/**
 * Produce metrics for Hibernate ORM
 * Avoid hard dependencies in main processor
 */
public final class HibernateOrmMetrics {
    @BuildStep
    @Record(RUNTIME_INIT)
    public void metrics(HibernateOrmConfig config,
            HibernateMetricsRecorder recorder,
            BuildProducer<MetricsFactoryConsumerBuildItem> datasourceMetrics) {

        // IF MP metrics, Hibernate metrics and Hibernate statistics are all enabled
        if (config.metricsEnabled && config.statistics.orElse(true)) {
            datasourceMetrics.produce(new MetricsFactoryConsumerBuildItem(
                    recorder.registerMetrics()));
        }
    }
}
