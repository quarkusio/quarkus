package io.quarkus.hibernate.orm.deployment.metrics;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.Optional;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.runtime.metrics.HibernateMetricsRecorder;
import io.quarkus.runtime.metrics.MetricsFactory;

/**
 * Produce metrics for Hibernate ORM
 * Avoid hard dependencies in main processor
 */
public final class HibernateOrmMetricsProcessor {
    @BuildStep
    @Record(RUNTIME_INIT)
    public void metrics(HibernateOrmConfig config,
            HibernateMetricsRecorder metricsRecorder,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            BuildProducer<MetricsFactoryConsumerBuildItem> datasourceMetrics) {

        // IF Hibernate metrics and Hibernate statistics are enabled
        // then define a consumer. It will only be invoked if metrics is enabled
        if (config.metricsEnabled && config.statistics.orElse(true) && metricsConfiguration.isPresent()) {
            MetricsFactoryConsumerBuildItem buildItem;
            if (metricsConfiguration.get().metricsSupported(MetricsFactory.MICROMETER)) {
                buildItem = new MetricsFactoryConsumerBuildItem(metricsRecorder.registerMicrometerMetrics());
            } else {
                buildItem = new MetricsFactoryConsumerBuildItem(metricsRecorder.registerMPMetrics());
            }
            datasourceMetrics.produce(buildItem);
        }
    }
}
