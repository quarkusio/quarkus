package io.quarkus.agroal.runtime.metrics;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSourceMetrics;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.metrics.MetricsFactory;

/**
 * Subclasses are created and registered as {literal @}Dependent beans for each
 * datasource that has metrics enabled.
 */
@Recorder
public class AgroalMetricsRecorder {
    private static final Logger log = Logger.getLogger(AgroalMetricsRecorder.class);

    static Function<Supplier<Duration>, Long> convertToMillis = new Function<Supplier<Duration>, Long>() {
        @Override
        public Long apply(Supplier<Duration> durationSupplier) {
            return durationSupplier.get().toMillis();
        }
    };

    /* RUNTIME_INIT */
    public Consumer<MetricsFactory> registerDataSourceMetrics(String dataSourceName) {
        return new Consumer<MetricsFactory>() {
            @Override
            public void accept(MetricsFactory metricsFactory) {
                DataSources dataSources = Arc.container().instance(DataSources.class).get();
                if (!dataSources.getActiveDataSourceNames().contains(dataSourceName)) {
                    log.debug("Not registering metrics for datasource '" + dataSourceName + "'"
                            + " as the datasource has been deactivated in the configuration");
                    return;
                }

                String tagValue = DataSourceUtil.isDefault(dataSourceName) ? "default" : dataSourceName;
                AgroalDataSourceMetrics metrics = dataSources.getDataSource(dataSourceName).getMetrics();

                metricsFactory.builder("agroal.active.count")
                        .description(
                                "Number of active connections. These connections are in use and not available to be acquired.")
                        .tag("datasource", tagValue)
                        .buildGauge(metrics::activeCount);
                metricsFactory.builder("agroal.available.count")
                        .description("Number of idle connections in the pool, available to be acquired.")
                        .tag("datasource", tagValue)
                        .buildGauge(metrics::availableCount);
                metricsFactory.builder("agroal.max.used.count")
                        .description("Maximum number of connections active simultaneously.")
                        .tag("datasource", tagValue)
                        .buildGauge(metrics::maxUsedCount);
                metricsFactory.builder("agroal.awaiting.count")
                        .description("Approximate number of threads blocked, waiting to acquire a connection.")
                        .tag("datasource", tagValue)
                        .buildGauge(metrics::awaitingCount);

                metricsFactory.builder("agroal.acquire.count")
                        .description("Number of times an acquire operation succeeded.")
                        .tag("datasource", tagValue)
                        .buildCounter(metrics::acquireCount);
                metricsFactory.builder("agroal.creation.count")
                        .description("Number of created connections.")
                        .tag("datasource", tagValue)
                        .buildCounter(metrics::creationCount);
                metricsFactory.builder("agroal.leak.detection.count")
                        .description("Number of times a leak was detected. A single connection can be detected multiple times.")
                        .tag("datasource", tagValue)
                        .buildCounter(metrics::leakDetectionCount);
                metricsFactory.builder("agroal.destroy.count")
                        .description("Number of destroyed connections.")
                        .tag("datasource", tagValue)
                        .buildCounter(metrics::destroyCount);
                metricsFactory.builder("agroal.flush.count")
                        .description("Number of connections removed from the pool, not counting invalid / idle.")
                        .tag("datasource", tagValue)
                        .buildCounter(metrics::flushCount);
                metricsFactory.builder("agroal.invalid.count")
                        .description("Number of connections removed from the pool for being idle.")
                        .tag("datasource", tagValue)
                        .buildCounter(metrics::invalidCount);
                metricsFactory.builder("agroal.reap.count")
                        .description("Number of connections removed from the pool for being idle.")
                        .tag("datasource", tagValue)
                        .buildCounter(metrics::reapCount);

                metricsFactory.builder("agroal.blocking.time.average")
                        .description("Average time an application waited to acquire a connection.")
                        .tag("datasource", tagValue)
                        .unit("milliseconds")
                        .buildGauge(metrics::blockingTimeAverage, convertToMillis);
                metricsFactory.builder("agroal.blocking.time.max")
                        .description("Maximum time an application waited to acquire a connection.")
                        .tag("datasource", tagValue)
                        .unit("milliseconds")
                        .buildGauge(metrics::blockingTimeMax, convertToMillis);
                metricsFactory.builder("agroal.blocking.time.total")
                        .description("Total time applications waited to acquire a connection.")
                        .tag("datasource", tagValue)
                        .unit("milliseconds")
                        .buildGauge(metrics::blockingTimeTotal, convertToMillis);
                metricsFactory.builder("agroal.creation.time.average")
                        .description("Average time for a connection to be created.")
                        .tag("datasource", tagValue)
                        .unit("milliseconds")
                        .buildGauge(metrics::creationTimeAverage, convertToMillis);
                metricsFactory.builder("agroal.creation.time.max")
                        .description("Maximum time for a connection to be created.")
                        .tag("datasource", tagValue)
                        .unit("milliseconds")
                        .buildGauge(metrics::creationTimeMax, convertToMillis);
                metricsFactory.builder("agroal.creation.time.total")
                        .description("Total time waiting for connections to be created.")
                        .tag("datasource", tagValue)
                        .unit("milliseconds")
                        .buildGauge(metrics::creationTimeTotal, convertToMillis);
            }
        };
    }
}
