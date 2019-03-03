package org.infinispan.quarkus.hibernate.cache;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.infinispan.quarkus.hibernate.cache.VersionedEntry.ComputeFn;
import org.jboss.logging.Logger;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import java.util.Comparator;
import java.util.function.Supplier;

final class NonStrictDataAccess implements InternalDataAccess {

    private static final Logger log = Logger.getLogger(NonStrictDataAccess.class);
    private static final boolean trace = log.isTraceEnabled();

    private final InternalCache cache;
    private final InternalRegion internalRegion;
    private final Comparator versionComparator;
    private final Supplier<Long> nextTimestamp;

    NonStrictDataAccess(InternalCache cache, InternalRegion internalRegion, Comparator versionComparator, RegionFactory regionFactory) {
        this.cache = cache;
        this.internalRegion = internalRegion;
        this.versionComparator = versionComparator;
        this.nextTimestamp = regionFactory::nextTimestamp;
    }

    @Override
    public Object get(Object session, Object key, long txTimestamp) {
        if (txTimestamp < internalRegion.getLastRegionInvalidation()) {
            return null;
        }
        Object value = cache.getOrNull(key);
        if (value instanceof VersionedEntry) {
            return ((VersionedEntry) value).getValue();
        }
        return value;
    }

    @Override
    public boolean putFromLoad(Object session, Object key, Object value, long txTimestamp, Object version) {
        return putFromLoad(session, key, value, txTimestamp, version, false);
    }

    @Override
    public boolean putFromLoad(Object session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride) {
        long lastRegionInvalidation = internalRegion.getLastRegionInvalidation();
        if (txTimestamp < lastRegionInvalidation) {
            log.tracef("putFromLoad not executed since tx started at %d, before last region invalidation finished = %d", txTimestamp, lastRegionInvalidation);
            return false;
        }
        assert version != null;

        if (minimalPutOverride) {
            Object prev = cache.getOrNull(key);
            if (prev != null) {
                Object oldVersion = getVersion(prev);
                if (oldVersion != null) {
                    if (versionComparator.compare(version, oldVersion) <= 0) {
                        if (trace) {
                            log.tracef("putFromLoad not executed since version(%s) <= oldVersion(%s)", version, oldVersion);
                        }
                        return false;
                    }
                } else if (prev instanceof VersionedEntry && txTimestamp <= ((VersionedEntry) prev).getTimestamp()) {
                    if (trace) {
                        log.tracef("putFromLoad not executed since tx started at %d and entry was invalidated at %d",
                                txTimestamp, ((VersionedEntry) prev).getTimestamp());
                    }
                    return false;
                }
            }
        }
        // Even if value is instanceof CacheEntry, we have to wrap it in VersionedEntry and add transaction timestamp.
        // Otherwise, old eviction record wouldn't be overwritten.
        final VersionedEntry versioned = new VersionedEntry(value, version, txTimestamp);
        cache.compute(key, new ComputeFn(versioned, internalRegion));
        return true;
    }

    @Override
    public boolean insert(Object session, Object key, Object value, Object version) {
        return false;
    }

    @Override
    public boolean update(Object session, Object key, Object value, Object currentVersion, Object previousVersion) {
        return false;
    }

    @Override
    public void remove(Object session, Object key) {
        // there's no 'afterRemove', so we have to use our own synchronization
        // the API does not provide version of removed item but we can't load it from the cache
        // as that would be prone to race conditions - if the entry was updated in the meantime
        // the remove could be discarded and we would end up with stale record
        final TransactionCoordinator transactionCoordinator = ((SharedSessionContractImplementor) session).getTransactionCoordinator();
        RemovalSynchronization sync = new RemovalSynchronization(key, transactionCoordinator);
        transactionCoordinator.getLocalSynchronizations().registerSynchronization(sync);
    }

    @Override
    public void removeAll() {
        internalRegion.beginInvalidation();
        try {
            cache.invalidateAll();
        } finally {
            internalRegion.endInvalidation();
        }
    }

    @Override
    public void evict(Object key) {
        cache.compute(key, new ComputeFn(new VersionedEntry(nextTimestamp.get()), internalRegion));
    }

    @Override
    public void evictAll() {
        internalRegion.beginInvalidation();
        try {
            cache.invalidateAll();
        } finally {
            internalRegion.endInvalidation();
        }
    }

    @Override
    public boolean afterInsert(Object session, Object key, Object value, Object version) {
        assert value != null;
        assert version != null;
        final long timestamp = ((SharedSessionContractImplementor) session).getTransactionStartTimestamp();
        cache.compute(key, new ComputeFn(new VersionedEntry(value, version, timestamp), internalRegion));
        return true;
    }

    @Override
    public boolean afterUpdate(Object session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
        assert value != null;
        assert currentVersion != null;
        final long timestamp = ((SharedSessionContractImplementor) session).getTransactionStartTimestamp();
        cache.compute(key, new ComputeFn(new VersionedEntry(value, currentVersion, timestamp), internalRegion));
        return true;
    }

    private Object getVersion(Object value) {
        if (value instanceof CacheEntry) {
            return ((CacheEntry) value).getVersion();
        } else if (value instanceof VersionedEntry) {
            return ((VersionedEntry) value).getVersion();
        }
        return null;
    }

    private final class RemovalSynchronization implements Synchronization {

        private final Object key;
        private final TransactionCoordinator transactionCoordinator;

        private RemovalSynchronization(Object key, TransactionCoordinator transactionCoordinator) {
            this.key = key;
            this.transactionCoordinator = transactionCoordinator;
        }

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            switch (status) {
                case Status.STATUS_COMMITTING:
                case Status.STATUS_COMMITTED:
                    invokeIsolated(true);
                    break;
                default:
                    // it would be nicer to react only on ROLLING_BACK and ROLLED_BACK statuses
                    // but TransactionCoordinator gives us UNKNOWN on rollback
                    invokeIsolated(false);
                    break;
            }
        }

        protected void invokeIsolated(final boolean success) {
            try {
                // TODO: isolation without obtaining Connection -> needs HHH-9993
                final WorkExecutorVisitable<Void> work = (executor, connection) -> {
                    if (success) {
                        cache.compute(key, new ComputeFn(new VersionedEntry(nextTimestamp.get()), internalRegion));
                    }
                    return null;
                };

                transactionCoordinator.createIsolationDelegate().delegateWork(work, false);
            } catch (HibernateException e) {
                // silently fail any exceptions
                if (log.isTraceEnabled()) {
                    log.trace("Exception during query cache update", e);
                }
            }
        }

    }

}
