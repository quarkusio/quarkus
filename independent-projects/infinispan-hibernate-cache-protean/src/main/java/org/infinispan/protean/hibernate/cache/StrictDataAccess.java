package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.jboss.logging.Logger;

import javax.transaction.Status;
import javax.transaction.Synchronization;

// TODO temporarily public, for correctness testing checks...
public final class StrictDataAccess implements InternalDataAccess {

    private static final Logger log = Logger.getLogger(StrictDataAccess.class);
    private static final boolean trace = log.isTraceEnabled();

    private final InternalCache cache;
    private final PutFromLoadValidator putValidator;
    private final InternalRegion internalRegion;

    StrictDataAccess(InternalCache cache, PutFromLoadValidator putValidator, InternalRegion internalRegion) {
        this.cache = cache;
        this.putValidator = putValidator;
        this.internalRegion = internalRegion;
    }

    @Override
    public Object get(Object session, Object key, long txTimestamp) {
        if (!internalRegion.checkValid()) {
            if (trace) {
                log.tracef("Region %s not valid", internalRegion.getName());
            }
            return null;
        }
        final Object val = cache.getOrNull(key);
        if (val == null && session != null) {
            putValidator.registerPendingPut(session, key, txTimestamp);
        }
        return val;
    }

    @Override
    public boolean putFromLoad(Object session, Object key, Object value, long txTimestamp, Object version) {
        return putFromLoad(session, key, value, txTimestamp, version, false);
    }

    @Override
    public boolean putFromLoad(Object session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride) {
        if (!internalRegion.checkValid()) {
            if (trace) {
                log.tracef("Region %s not valid", internalRegion.getName());
            }
            return false;
        }

        if (minimalPutOverride && cache.getOrNull(key) != null) {
            return false;
        }

        PutFromLoadValidator.Lock lock = putValidator.acquirePutFromLoadLock(session, key, txTimestamp);
        if (lock == null) {
            if (trace) {
                log.tracef("Put from load lock not acquired for key %s", key);
            }
            return false;
        }

        try {
            cache.putIfAbsent(key, value);
        } finally {
            putValidator.releasePutFromLoadLock(key, lock);
        }

        return true;
    }

    @Override
    public boolean insert(Object session, Object key, Object value, Object version) {
        if (!internalRegion.checkValid()) {
            return false;
        }
        write(session, key, value);
        return true;
    }

    @Override
    public boolean update(Object session, Object key, Object value, Object currentVersion, Object previousVersion) {
        // We update whether or not the region is valid.
        write(session, key, value);
        return true;
    }

    @Override
    public void remove(Object session, Object key) {
        // We update whether or not the region is valid. Other nodes
        // may have already restored the region so they need to
        // be informed of the change.
        write(session, key, null);
    }

    @Override
    public void removeAll() {
        try {
            if (!putValidator.beginInvalidatingRegion()) {
                log.error("Failed to invalidate pending putFromLoad calls for region " + internalRegion.getName());
            }
            cache.invalidateAll();
        } finally {
            putValidator.endInvalidatingRegion();
        }
    }

    @Override
    public void evict(Object key) {
        cache.invalidate(key);
    }

    @Override
    public void evictAll() {
        try {
            if (!putValidator.beginInvalidatingRegion()) {
                log.error("Failed to invalidate pending putFromLoad calls for region " + internalRegion.getName());
            }

            // Invalidate the local region
            internalRegion.clear();
        } finally {
            putValidator.endInvalidatingRegion();
        }
    }

    @Override
    public boolean afterInsert(Object session, Object key, Object value, Object version) {
        return false;
    }

    @Override
    public boolean afterUpdate(Object session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
        return false;
    }

    private void write(Object session, Object key, Object value) {
        // We need to be invalidating even for regular writes; if we were not and the write was followed by eviction
        // (or any other invalidation), naked put that was started after the eviction ended but before this insert/update
        // ended could insert the stale entry into the cache (since the entry was removed by eviction).
        // Lock owner is not serialized in local mode so we can use anything (only equals and hashCode are needed)
        Object lockOwner = new Object();
        registerLocalInvalidation(session, lockOwner, key);
        if (!putValidator.beginInvalidatingWithPFER(lockOwner, key, value)) {
            throw new CacheException(String.format(
                    "Failed to invalidate pending putFromLoad calls for key %s from region %s"
                    , key, internalRegion.getName()
            ));
        }
        // Make use of the simple cache mode here
        cache.invalidate(key);
    }

    protected void registerLocalInvalidation(Object session, Object lockOwner, Object key) {
        TransactionCoordinator transactionCoordinator = ((SharedSessionContractImplementor) session).getTransactionCoordinator();
        if (transactionCoordinator == null) {
            return;
        }
        if (trace) {
            log.tracef("Registering synchronization on transaction in %s, cache %s: %s", lockOwner, internalRegion.getName(), key);
        }
        transactionCoordinator.getLocalSynchronizations()
                .registerSynchronization(new LocalInvalidationSynchronization(putValidator, key, lockOwner));
    }

    private static final class LocalInvalidationSynchronization implements Synchronization {
        private static final Logger log = Logger.getLogger(LocalInvalidationSynchronization.class);
        private final static boolean trace = log.isTraceEnabled();

        private final Object lockOwner;
        private final PutFromLoadValidator validator;
        private final Object key;

        public LocalInvalidationSynchronization(PutFromLoadValidator validator, Object key, Object lockOwner) {
            assert lockOwner != null;
            this.validator = validator;
            this.key = key;
            this.lockOwner = lockOwner;
        }

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            if (trace) {
                log.tracef("After completion callback with status %d", status);
            }
            validator.endInvalidatingKey(lockOwner, key, status == Status.STATUS_COMMITTED || status == Status.STATUS_COMMITTING);
        }

    }

}
