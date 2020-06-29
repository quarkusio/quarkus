package io.quarkus.hibernate.orm.deployment.metrics;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.runtime.metrics.HibernateCounter;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;

/**
 * Responsible to produce all MetricBuildItem related to Hibernate ORM
 */
public final class HibernateOrmMetrics {

    @BuildStep
    public void metrics(HibernateOrmConfig config,
            BuildProducer<MetricBuildItem> metrics) {
        // TODO: When multiple PUs are supported, create metrics for each PU. For now we only assume the "default" PU.
        boolean metricsEnabled = config.metricsEnabled && config.statistics.orElse(true);
        metrics.produce(createMetricBuildItem("hibernate-orm.sessions.open",
                "Global number of sessions opened",
                "sessionsOpened",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.sessions.closed",
                "Global number of sessions closed",
                "sessionsClosed",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.sessions.closed",
                "Global number of sessions closed",
                "sessionsClosed",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.transactions",
                "The number of transactions we know to have completed",
                "transactionCount",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.transactions.successful",
                "The number of transactions we know to have been successful",
                "successfulTransactions",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.optimistic.lock.failures",
                "The number of Hibernate StaleObjectStateExceptions or JPA OptimisticLockExceptions that occurred.",
                "optimisticLockFailures",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.flushes",
                "Global number of flush operations executed (either manual or automatic).",
                "flushes",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.connections.obtained",
                "Get the global number of connections asked by the sessions " +
                        "(the actual number of connections used may be much smaller depending " +
                        "whether you use a connection pool or not)",
                "connectionsObtained",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.statements.prepared",
                "The number of prepared statements that were acquired",
                "statementsPrepared",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.statements.closed",
                "The number of prepared statements that were released",
                "statementsClosed",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.second-level-cache.puts",
                "Global number of cacheable entities/collections put in the cache",
                "secondLevelCachePuts",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.second-level-cache.hits",
                "Global number of cacheable entities/collections successfully retrieved from the cache",
                "secondLevelCacheHits",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.second-level-cache.misses",
                "Global number of cacheable entities/collections not found in the cache and loaded from the database.",
                "secondLevelCacheMisses",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.loaded",
                "Global number of entity loads",
                "entitiesLoaded",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.updated",
                "Global number of entity updates",
                "entitiesUpdated",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.inserted",
                "Global number of entity inserts",
                "entitiesInserted",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.deleted",
                "Global number of entity deletes",
                "entitiesDeleted",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.fetched",
                "Global number of entity fetches",
                "entitiesFetched",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.loaded",
                "Global number of collections loaded",
                "collectionsLoaded",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.updated",
                "Global number of collections updated",
                "collectionsUpdated",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.removed",
                "Global number of collections removed",
                "collectionsRemoved",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.recreated",
                "Global number of collections recreated",
                "collectionsRecreated",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.fetched",
                "Global number of collections fetched",
                "collectionsFetched",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.natural-id.queries.executions",
                "Global number of natural id queries executed against the database",
                "naturalIdQueriesExecutedToDatabase",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.natural-id.cache.hits",
                "Global number of cached natural id lookups successfully retrieved from cache",
                "naturalIdCacheHits",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.natural-id.cache.puts",
                "Global number of cacheable natural id lookups put in cache",
                "naturalIdCachePuts",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.natural-id.cache.misses",
                "Global number of cached natural id lookups *not* found in cache",
                "naturalIdCacheMisses",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.queries.executed",
                "Global number of executed queries",
                "queriesExecutedToDatabase",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.query-cache.puts",
                "Global number of cacheable queries put in cache",
                "queryCachePuts",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.query-cache.hits",
                "Global number of cached queries successfully retrieved from cache",
                "queryCacheHits",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.query-cache.misses",
                "Global number of cached queries *not* found in cache",
                "queryCacheMisses",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.timestamps-cache.puts",
                "Global number of timestamps put in cache",
                "updateTimestampsCachePuts",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.timestamps-cache.hits",
                "Global number of timestamps successfully retrieved from cache",
                "updateTimestampsCacheHits",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.timestamps-cache.misses",
                "Global number of timestamp requests that were not found in the cache",
                "updateTimestampsCacheMisses",
                metricsEnabled));
    }

    private MetricBuildItem createMetricBuildItem(String metricName, String description, String metric,
            boolean metricsEnabled) {
        return new MetricBuildItem(Metadata.builder()
                .withName(metricName)
                .withDescription(description)
                .withType(MetricType.COUNTER)
                .build(),
                new HibernateCounter("default", metric),
                metricsEnabled,
                "hibernate-orm");
    }

}
