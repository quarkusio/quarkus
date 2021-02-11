package io.quarkus.hibernate.orm.runtime.metrics;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.metrics.MetricsFactory;

/**
 * This recorder is invoked IFF Hibernate metrics and Hibernate statistics are enabled
 */
@Recorder
public class HibernateMetricsRecorder {
    private static final String SESSION_FACTORY_TAG_NAME = "entityManagerFactory";

    /* RUNTIME_INIT for metrics */
    public Consumer<MetricsFactory> consumeMetricsFactory() {
        return new Consumer<MetricsFactory>() {
            @Override
            public void accept(MetricsFactory metricsFactory) {
                JPAConfig jpaConfig = Arc.container().instance(JPAConfig.class).get();
                for (String puName : jpaConfig.getPersistenceUnits()) {
                    SessionFactory sessionFactory = jpaConfig.getEntityManagerFactory(puName).unwrap(SessionFactory.class);
                    if (sessionFactory != null) {
                        registerMetrics(metricsFactory, puName, sessionFactory.getStatistics());
                    }
                }
            }
        };
    }

    /**
     * Register MP Metrics
     * 
     * @param metricsFactory Quarkus MetricsFactory for generic metrics registration
     * @param puName Name of persistence unit
     * @param statistics Statistics MXBean for persistence unit
     */
    void registerMetrics(MetricsFactory metricsFactory, String puName, Statistics statistics) {

        // Session statistics
        createStatisticsCounter(metricsFactory, "hibernate.sessions.open",
                "Global number of sessions opened",
                puName, statistics, Statistics::getSessionOpenCount);
        createStatisticsCounter(metricsFactory, "hibernate.sessions.closed",
                "Global number of sessions closed",
                puName, statistics, Statistics::getSessionCloseCount);

        // Transaction statistics
        createStatisticsCounter(metricsFactory, "hibernate.transactions",
                "The number of transactions (see result for success or failure)",
                puName, statistics, Statistics::getSuccessfulTransactionCount,
                "result", "success");
        createStatisticsCounter(metricsFactory, "hibernate.transactions",
                "The number of transactions (see result for success or failure)",
                puName, statistics, s -> s.getTransactionCount() - s.getSuccessfulTransactionCount(),
                "result", "failure");
        createStatisticsCounter(metricsFactory, "hibernate.optimistic.failures",
                "The number of Hibernate StaleObjectStateExceptions or JPA OptimisticLockExceptions that occurred.",
                puName, statistics, Statistics::getOptimisticFailureCount);

        createStatisticsCounter(metricsFactory, "hibernate.flushes",
                "Global number of flush operations executed (either manual or automatic).",
                puName, statistics, Statistics::getFlushCount);
        createStatisticsCounter(metricsFactory, "hibernate.connections.obtained",
                "Get the global number of connections asked by the sessions " +
                        "(the actual number of connections used may be much smaller depending " +
                        "whether you use a connection pool or not)",
                puName, statistics, Statistics::getConnectCount);

        // Statements
        createStatisticsCounter(metricsFactory, "hibernate.statements",
                "The number of prepared statements (see status for prepared or closed)",
                puName, statistics, Statistics::getPrepareStatementCount,
                "status", "prepared");
        createStatisticsCounter(metricsFactory, "hibernate.statements",
                "The number of prepared statements (see status for prepared or closed)",
                puName, statistics, Statistics::getCloseStatementCount,
                "status", "closed");

        // Second Level Caching
        Arrays.stream(statistics.getSecondLevelCacheRegionNames())
                .filter(regionName -> this.hasDomainDataRegionStatistics(statistics, regionName))
                .forEach(regionName -> {
                    CacheRegionStatistics regionStatistics = statistics.getDomainDataRegionStatistics(regionName);
                    createStatisticsCounter(metricsFactory, "hibernate.second.level.cache.requests",
                            "The number of requests made to second level cache (see result for hit or miss)",
                            puName, regionStatistics, CacheRegionStatistics::getHitCount,
                            "result", "hit", "region", regionName);
                    createStatisticsCounter(metricsFactory, "hibernate.second.level.cache.requests",
                            "The number of requests made to second level cache (see result for hit or miss)",
                            puName, regionStatistics, CacheRegionStatistics::getMissCount,
                            "result", "miss", "region", regionName);
                    createStatisticsCounter(metricsFactory, "hibernate.second.level.cache.puts",
                            "The number of entities/collections put in the second level cache",
                            puName, regionStatistics, CacheRegionStatistics::getPutCount,
                            "region", regionName);
                });

        // Entity Information
        createStatisticsCounter(metricsFactory, "hibernate.entities.loads",
                "Global number of entity loads",
                puName, statistics, Statistics::getEntityLoadCount);
        createStatisticsCounter(metricsFactory, "hibernate.entities.updates",
                "Global number of entity updates",
                puName, statistics, Statistics::getEntityUpdateCount);
        createStatisticsCounter(metricsFactory, "hibernate.entities.inserts",
                "Global number of entity inserts",
                puName, statistics, Statistics::getEntityInsertCount);
        createStatisticsCounter(metricsFactory, "hibernate.entities.deletes",
                "Global number of entity deletes",
                puName, statistics, Statistics::getEntityDeleteCount);
        createStatisticsCounter(metricsFactory, "hibernate.entities.fetches",
                "Global number of entity fetches",
                puName, statistics, Statistics::getEntityFetchCount);

        // Collections
        createStatisticsCounter(metricsFactory, "hibernate.collections.loads",
                "Global number of collections loaded",
                puName, statistics, Statistics::getCollectionLoadCount);
        createStatisticsCounter(metricsFactory, "hibernate.collections.updates",
                "Global number of collections updated",
                puName, statistics, Statistics::getCollectionUpdateCount);
        createStatisticsCounter(metricsFactory, "hibernate.collections.deletes",
                "Global number of collections removed",
                puName, statistics, Statistics::getCollectionRemoveCount);
        createStatisticsCounter(metricsFactory, "hibernate.collections.recreates",
                "Global number of collections recreated",
                puName, statistics, Statistics::getCollectionRecreateCount);
        createStatisticsCounter(metricsFactory, "hibernate.collections.fetches",
                "Global number of collections fetched",
                puName, statistics, Statistics::getCollectionFetchCount);

        // Natural Id cache
        createStatisticsCounter(metricsFactory, "hibernate.natural.id.requests",
                "The number of natural id cache requests (see result for hit or miss)",
                puName, statistics, Statistics::getNaturalIdCacheHitCount,
                "result", "hit");
        createStatisticsCounter(metricsFactory, "hibernate.natural.id.cache.puts",
                "The number of cacheable natural id requests put in cache",
                puName, statistics, Statistics::getNaturalIdCachePutCount);
        createStatisticsCounter(metricsFactory, "hibernate.natural.id.requests",
                "The number of natural id cache requests (see result for hit or miss)",
                puName, statistics, Statistics::getNaturalIdCacheMissCount,
                "result", "miss");

        // Natural Id statistics
        createStatisticsCounter(metricsFactory, "hibernate.natural.id.executions",
                "The number of natural id query executions",
                puName, statistics, Statistics::getNaturalIdQueryExecutionCount);
        createTimeGauge(metricsFactory, "hibernate.query.natural.id.executions.max",
                "The maximum query time for natural id queries executed against the database",
                puName, statistics, Statistics::getNaturalIdQueryExecutionMaxTime);

        // Query statistics
        createStatisticsCounter(metricsFactory, "hibernate.query.executions",
                "The number of query executions",
                puName, statistics, Statistics::getQueryExecutionCount);
        createTimeGauge(metricsFactory, "hibernate.query.executions.max",
                "The maximum query time for queries executed against the database",
                puName, statistics, Statistics::getQueryExecutionMaxTime);

        // Query Cache
        createStatisticsCounter(metricsFactory, "hibernate.cache.query.requests",
                "The number of query cache requests (see result for hit or miss)",
                puName, statistics, Statistics::getQueryCacheHitCount,
                "result", "hit");
        createStatisticsCounter(metricsFactory, "hibernate.cache.query.requests",
                "The number of query cache requests (see result for hit or miss)",
                puName, statistics, Statistics::getQueryCacheMissCount,
                "result", "miss");
        createStatisticsCounter(metricsFactory, "hibernate.cache.query.puts",
                "The number of cacheable queries put in cache",
                puName, statistics, Statistics::getQueryCachePutCount);
        createStatisticsCounter(metricsFactory, "hibernate.cache.query.plan",
                "The number of query plan cache requests (see result for hit or miss)",
                puName, statistics, Statistics::getQueryPlanCacheHitCount,
                "result", "hit");
        createStatisticsCounter(metricsFactory, "hibernate.cache.query.plan",
                "The number of query plan cache requests (see result for hit or miss)",
                puName, statistics, Statistics::getQueryPlanCacheMissCount,
                "result", "miss");

        // Timestamp cache
        createStatisticsCounter(metricsFactory, "hibernate.cache.update.timestamps.requests",
                "The number of update timestamps cache requests (see result for hit or miss)",
                puName, statistics, Statistics::getUpdateTimestampsCacheHitCount,
                "result", "hit");
        createStatisticsCounter(metricsFactory, "hibernate.cache.update.timestamps.requests",
                "The number of update timestamps cache requests (see result for hit or miss)",
                puName, statistics, Statistics::getUpdateTimestampsCacheMissCount,
                "result", "miss");
        createStatisticsCounter(metricsFactory, "hibernate.cache.update.timestamps.puts",
                "The number of update timestamps put in cache",
                puName, statistics, Statistics::getUpdateTimestampsCachePutCount);
    }

    <T> void createStatisticsCounter(MetricsFactory metricsFactory, String metricName, String description,
            String puName, T statistics, Function<T, Long> f, String... tags) {

        createBuilder(metricsFactory, metricName, description, puName, tags)
                .buildCounter(statistics, f);
    }

    void createTimeGauge(MetricsFactory metricsFactory, String metricName, String description,
            String puName, Statistics statistics, Function<Statistics, Long> f, String... tags) {

        createBuilder(metricsFactory, metricName, description, puName, tags)
                .unit(TimeUnit.MILLISECONDS.toString())
                .buildGauge(statistics, f);
    }

    MetricsFactory.MetricBuilder createBuilder(MetricsFactory metricsFactory, String metricName, String description,
            String puName, String... tags) {
        MetricsFactory.MetricBuilder builder = metricsFactory.builder(metricName)
                .description(description)
                .tag(SESSION_FACTORY_TAG_NAME, puName);
        // Add (optional) additional tags
        if (tags.length > 0 && tags.length % 2 == 0) {
            for (int i = 0; i < tags.length; i = i + 2) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        return builder;
    }

    private boolean hasDomainDataRegionStatistics(Statistics statistics, String regionName) {
        // In 5.1/5.2, getSecondLevelCacheStatistics returns null if the region can't be resolved.
        // In 5.3, getDomainDataRegionStatistics (a new method) will throw an IllegalArgumentException
        // if the region can't be resolved.
        try {
            return statistics.getDomainDataRegionStatistics(regionName) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
