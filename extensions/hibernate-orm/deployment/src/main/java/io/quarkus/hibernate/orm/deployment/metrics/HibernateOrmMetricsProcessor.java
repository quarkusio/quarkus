package io.quarkus.hibernate.orm.deployment.metrics;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.List;
import java.util.Optional;

import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.PersistenceProviderSetUpBuildItem;
import io.quarkus.hibernate.orm.runtime.metrics.HibernateMetricsRecorder;

/**
 * Produce metrics for Hibernate ORM
 * Avoid hard dependencies in main processor
 */
@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class HibernateOrmMetricsProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    public void metrics(HibernateOrmConfig config,
            HibernateMetricsRecorder metricsRecorder,
            List<PersistenceProviderSetUpBuildItem> persistenceUnitsStarted,
            List<JdbcDataSourceSchemaReadyBuildItem> jdbcDataSourceSchemaReadyBuildItems,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            BuildProducer<MetricsFactoryConsumerBuildItem> datasourceMetrics) {

        // IF Hibernate metrics and Hibernate statistics are enabled
        // then define a consumer. It will only be invoked if metrics is enabled
        if (config.metrics().enabled() && config.statistics().orElse(true) && metricsConfiguration.isPresent()) {
            datasourceMetrics.produce(new MetricsFactoryConsumerBuildItem(metricsRecorder.consumeMetricsFactory()));
        }
    }
}
