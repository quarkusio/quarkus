package io.quarkus.hibernate.orm.runtime.metrics;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.SessionFactory;
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

    /* RUNTIME_INIT if Micrometer Metrics is present */
    public Consumer<MetricsFactory> registerMicrometerMetrics() {
        return new Consumer<MetricsFactory>() {
            @Override
            public void accept(MetricsFactory metricsFactory) {
                JPAConfig jpaConfig = Arc.container().instance(JPAConfig.class).get();
                for (String puName : jpaConfig.getPersistenceUnits()) {
                    SessionFactory sessionFactory = jpaConfig.getEntityManagerFactory(puName).unwrap(SessionFactory.class);
                    if (sessionFactory != null) {
                        HibernateMicrometerMetrics.registerMeterBinders(puName, sessionFactory);
                    }
                }
            }
        };
    }

    /* RUNTIME_INIT if MP Metrics is present */
    public Consumer<MetricsFactory> registerMPMetrics() {
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
        createStatisticsCounter(metricsFactory, "hibernate-orm.sessions.open",
                "Global number of sessions opened",
                puName, statistics, Statistics::getSessionOpenCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.sessions.closed",
                "Global number of sessions closed",
                puName, statistics, Statistics::getSessionCloseCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.transactions",
                "The number of transactions we know to have completed",
                puName, statistics, Statistics::getTransactionCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.transactions.successful",
                "The number of transactions we know to have been successful",
                puName, statistics, Statistics::getSuccessfulTransactionCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.optimistic.lock.failures",
                "The number of Hibernate StaleObjectStateExceptions or JPA OptimisticLockExceptions that occurred.",
                puName, statistics, Statistics::getOptimisticFailureCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.flushes",
                "Global number of flush operations executed (either manual or automatic).",
                puName, statistics, Statistics::getFlushCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.connections.obtained",
                "Get the global number of connections asked by the sessions " +
                        "(the actual number of connections used may be much smaller depending " +
                        "whether you use a connection pool or not)",
                puName, statistics, Statistics::getConnectCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.statements.prepared",
                "The number of prepared statements that were acquired",
                puName, statistics, Statistics::getPrepareStatementCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.statements.closed",
                "The number of prepared statements that were released",
                puName, statistics, Statistics::getCloseStatementCount);

        createStatisticsCounter(metricsFactory, "hibernate-orm.second-level-cache.puts",
                "Global number of cacheable entities/collections put in the cache",
                puName, statistics, Statistics::getSecondLevelCachePutCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.second-level-cache.hits",
                "Global number of cacheable entities/collections successfully retrieved from the cache",
                puName, statistics, Statistics::getSecondLevelCacheHitCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.second-level-cache.misses",
                "Global number of cacheable entities/collections not found in the cache and loaded from the database.",
                puName, statistics, Statistics::getSecondLevelCacheMissCount);

        createStatisticsCounter(metricsFactory, "hibernate-orm.entities.loaded",
                "Global number of entity loads",
                puName, statistics, Statistics::getEntityLoadCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.entities.updated",
                "Global number of entity updates",
                puName, statistics, Statistics::getEntityUpdateCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.entities.inserted",
                "Global number of entity inserts",
                puName, statistics, Statistics::getEntityInsertCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.entities.deleted",
                "Global number of entity deletes",
                puName, statistics, Statistics::getEntityDeleteCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.entities.fetched",
                "Global number of entity fetches",
                puName, statistics, Statistics::getEntityFetchCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.collections.loaded",
                "Global number of collections loaded",
                puName, statistics, Statistics::getCollectionLoadCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.collections.updated",
                "Global number of collections updated",
                puName, statistics, Statistics::getCollectionUpdateCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.collections.removed",
                "Global number of collections removed",
                puName, statistics, Statistics::getCollectionRemoveCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.collections.recreated",
                "Global number of collections recreated",
                puName, statistics, Statistics::getCollectionRecreateCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.collections.fetched",
                "Global number of collections fetched",
                puName, statistics, Statistics::getCollectionFetchCount);

        createStatisticsCounter(metricsFactory, "hibernate-orm.natural-id.queries.executions",
                "Global number of natural id queries executed against the database",
                puName, statistics, Statistics::getNaturalIdQueryExecutionCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.natural-id.cache.hits",
                "Global number of cached natural id lookups successfully retrieved from cache",
                puName, statistics, Statistics::getNaturalIdCacheHitCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.natural-id.cache.puts",
                "Global number of cacheable natural id lookups put in cache",
                puName, statistics, Statistics::getNaturalIdCachePutCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.natural-id.cache.misses",
                "Global number of cached natural id lookups *not* found in cache",
                puName, statistics, Statistics::getNaturalIdCacheMissCount);

        createStatisticsCounter(metricsFactory, "hibernate-orm.queries.executed",
                "Global number of executed queries",
                puName, statistics, Statistics::getQueryExecutionCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.query-cache.puts",
                "Global number of cacheable queries put in cache",
                puName, statistics, Statistics::getQueryCachePutCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.query-cache.hits",
                "Global number of cached queries successfully retrieved from cache",
                puName, statistics, Statistics::getQueryCacheHitCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.query-cache.misses",
                "Global number of cached queries *not* found in cache",
                puName, statistics, Statistics::getQueryCacheMissCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.timestamps-cache.puts",
                "Global number of timestamps put in cache",
                puName, statistics, Statistics::getUpdateTimestampsCachePutCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.timestamps-cache.hits",
                "Global number of timestamps successfully retrieved from cache",
                puName, statistics, Statistics::getUpdateTimestampsCacheHitCount);
        createStatisticsCounter(metricsFactory, "hibernate-orm.timestamps-cache.misses",
                "Global number of timestamp requests that were not found in the cache",
                puName, statistics, Statistics::getUpdateTimestampsCacheMissCount);
    }

    void createStatisticsCounter(MetricsFactory metricsFactory, String metricName, String description,
            String puName, Statistics statistics, Function<Statistics, Long> f) {
        metricsFactory.builder(metricName)
                .description(description)
                .tag(SESSION_FACTORY_TAG_NAME, puName)
                .buildCounter(statistics, f);
    }
}
