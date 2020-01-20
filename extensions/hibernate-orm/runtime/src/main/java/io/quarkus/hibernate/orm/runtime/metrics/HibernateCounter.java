package io.quarkus.hibernate.orm.runtime.metrics;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.JPAConfig;

public class HibernateCounter implements org.eclipse.microprofile.metrics.Counter {

    private volatile SessionFactory sessionFactory;
    private String persistenceUnitName;
    private String metric;

    public HibernateCounter() {
    }

    public HibernateCounter(String persistenceUnitName, String metric) {
        this.persistenceUnitName = persistenceUnitName;
        this.metric = metric;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public void setPersistenceUnitName(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    @Override
    public long getCount() {
        Statistics statistics = getSessionFactory().getStatistics();
        switch (metric) {
            case "sessionsOpened":
                return statistics.getSessionOpenCount();
            case "sessionsClosed":
                return statistics.getSessionCloseCount();
            case "transactionCount":
                return statistics.getTransactionCount();
            case "successfulTransactions":
                return statistics.getSuccessfulTransactionCount();
            case "optimisticLockFailures":
                return statistics.getOptimisticFailureCount();
            case "flushes":
                return statistics.getFlushCount();
            case "connectionsObtained":
                return statistics.getConnectCount();
            case "statementsPrepared":
                return statistics.getPrepareStatementCount();
            case "statementsClosed":
                return statistics.getCloseStatementCount();
            case "secondLevelCachePuts":
                return statistics.getSecondLevelCachePutCount();
            case "secondLevelCacheHits":
                return statistics.getSecondLevelCacheHitCount();
            case "secondLevelCacheMisses":
                return statistics.getSecondLevelCacheMissCount();
            case "entitiesLoaded":
                return statistics.getEntityLoadCount();
            case "entitiesUpdated":
                return statistics.getEntityUpdateCount();
            case "entitiesInserted":
                return statistics.getEntityInsertCount();
            case "entitiesDeleted":
                return statistics.getEntityDeleteCount();
            case "entitiesFetched":
                return statistics.getEntityFetchCount();
            case "collectionsLoaded":
                return statistics.getCollectionLoadCount();
            case "collectionsUpdated":
                return statistics.getCollectionUpdateCount();
            case "collectionsRemoved":
                return statistics.getCollectionRemoveCount();
            case "collectionsRecreated":
                return statistics.getCollectionRecreateCount();
            case "collectionsFetched":
                return statistics.getCollectionFetchCount();
            case "naturalIdQueriesExecutedToDatabase":
                return statistics.getNaturalIdQueryExecutionCount();
            case "naturalIdCachePuts":
                return statistics.getNaturalIdCachePutCount();
            case "naturalIdCacheHits":
                return statistics.getNaturalIdCacheHitCount();
            case "naturalIdCacheMisses":
                return statistics.getNaturalIdCacheMissCount();
            case "queriesExecutedToDatabase":
                return statistics.getQueryExecutionCount();
            case "queryCachePuts":
                return statistics.getQueryCachePutCount();
            case "queryCacheHits":
                return statistics.getQueryCacheHitCount();
            case "queryCacheMisses":
                return statistics.getQueryCacheMissCount();
            case "updateTimestampsCachePuts":
                return statistics.getUpdateTimestampsCachePutCount();
            case "updateTimestampsCacheHits":
                return statistics.getUpdateTimestampsCacheHitCount();
            case "updateTimestampsCacheMisses":
                return statistics.getUpdateTimestampsCacheMissCount();
            default:
                throw new IllegalArgumentException("Unknown data source metric");
        }
    }

    @Override
    public void inc() {
        throw new IllegalStateException(
                "Hibernate metrics values are computed from Hibernate statistics objects and should not be updated manually");
    }

    @Override
    public void inc(long n) {
        throw new IllegalStateException(
                "Hibernate metrics values are computed from Hibernate statistics objects and should not be updated manually");
    }

    private SessionFactory getSessionFactory() {
        SessionFactory sfLocal = sessionFactory;
        if (sfLocal == null) {
            synchronized (this) {
                sfLocal = sessionFactory;
                if (sfLocal == null) {
                    sessionFactory = sfLocal = Arc.container().instance(JPAConfig.class).get()
                            .getEntityManagerFactory(persistenceUnitName != null ? persistenceUnitName : "default")
                            .unwrap(SessionFactory.class);
                }
            }
        }
        return sfLocal;
    }

}
