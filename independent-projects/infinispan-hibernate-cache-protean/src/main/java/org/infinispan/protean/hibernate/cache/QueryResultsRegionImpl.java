package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.stat.CacheRegionStatistics;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

final class QueryResultsRegionImpl implements QueryResultsRegion, ExtendedStatisticsSupport {

    private static final Logger log = Logger.getLogger(QueryResultsRegionImpl.class);

    private final InternalCache cache;
    private final String name;
    private final InternalRegionImpl internalRegion;
    private final RegionFactory regionFactory;

    private final ConcurrentMap<Object, Map> transactionContext = new ConcurrentHashMap<>();

    public QueryResultsRegionImpl(InternalCache cache, String name, InfinispanRegionFactory regionFactory) {
        this.cache = cache;
        this.name = name;
        this.regionFactory = regionFactory;
        this.internalRegion = new InternalRegionImpl(this);
    }

    @Override
    public Object getFromCache(Object key, SharedSessionContractImplementor session) {
        if (!internalRegion.checkValid()) {
            return null;
        }

        Object result = null;
        Map map = transactionContext.get(session);
        if (map != null) {
            result = map.get(key);
        }
        if (result == null) {
            result = cache.getOrNull(key);
        }
        return result;
    }

    @Override
    public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
        if (!internalRegion.checkValid()) {
            return;
        }
        // See HHH-7898: Even with FAIL_SILENTLY flag, failure to write in transaction
        // fails the whole transaction. It is an Infinispan quirk that cannot be fixed
        // ISPN-5356 tracks that. This is because if the transaction continued the
        // value could be committed on backup owners, including the failed operation,
        // and the result would not be consistent.
        Sync sync = (Sync) session.getCacheTransactionSynchronization();
        if (sync != null && session.isTransactionInProgress()) {
            sync.registerAfterCommit(new PostTransactionQueryUpdate(session, key, value));
            // no need to synchronize as the transaction will be accessed by only one thread
            transactionContext.computeIfAbsent(session, k -> new HashMap<>()).put(key, value);
        } else {
            cache.put(key, value);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RegionFactory getRegionFactory() {
        return regionFactory;
    }

    @Override
    public void clear() {
        transactionContext.clear();
        // Invalidate the local region
        internalRegion.beginInvalidation();
        runInvalidation();
        internalRegion.endInvalidation();
    }

    private void runInvalidation() {
        log.tracef("Non-transactional, clear in one go");
        cache.invalidateAll();
    }

    @Override
    public void destroy() throws CacheException {
    }

    @Override
    public long getElementCountInMemory() {
        return cache.size(null);
    }

    @Override
    public long getElementCountOnDisk() {
        return CacheRegionStatistics.NO_EXTENDED_STAT_SUPPORT_RETURN;
    }

    @Override
    public long getSizeInMemory() {
        return CacheRegionStatistics.NO_EXTENDED_STAT_SUPPORT_RETURN;
    }

    private class PostTransactionQueryUpdate implements Function<Boolean, CompletableFuture<?>> {
        private final Object session;
        private final Object key;
        private final Object value;

        PostTransactionQueryUpdate(Object session, Object key, Object value) {
            this.session = session;
            this.key = key;
            this.value = value;
        }

        @Override
        public CompletableFuture<?> apply(Boolean success) {
            transactionContext.remove(session);
            if (success) {
                cache.put(key, value);
                return CompletableFuture.completedFuture(null);
            } else {
                return null;
            }
        }
    }

}
