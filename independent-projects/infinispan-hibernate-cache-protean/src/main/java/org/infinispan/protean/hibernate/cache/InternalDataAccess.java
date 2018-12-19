package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.access.SoftLock;

interface InternalDataAccess {

    Object get(Object session, Object key, long txTimestamp);

    boolean putFromLoad(Object session, Object key, Object value, long txTimestamp, Object version);

    boolean putFromLoad(Object session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride);

    boolean insert(Object session, Object key, Object value, Object version);

    boolean update(Object session, Object key, Object value, Object currentVersion, Object previousVersion);

    void remove(Object session, Object key);

    void removeAll();

    void evict(Object key);

    void evictAll();

    boolean afterInsert(Object session, Object key, Object value, Object version);

    boolean afterUpdate(Object session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock);

}
