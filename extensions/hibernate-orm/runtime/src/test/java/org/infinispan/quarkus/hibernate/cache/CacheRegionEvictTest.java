package org.infinispan.quarkus.hibernate.cache;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CacheRegionEvictTest {

    @Test
    public void testReadWriteAccess() {
        CacheRegionTesting testing = CacheRegionTesting.cacheRegion("test", new HashMap());
        SharedSessionContractImplementor session = testing.session;

        EntityDataAccess cacheAccess = testing.entityCache(AccessType.READ_WRITE);

        putFromLoad(cacheAccess, session);
        assertEquals(3, testing.region().getElementCountInMemory());
        getCacheFound(session, cacheAccess);

        cacheAccess.evictAll();
        assertEquals(0, testing.region().getElementCountInMemory());
        getCacheNotFound(session, cacheAccess);

        putFromLoad(cacheAccess, session);
        assertEquals(0, testing.region().getElementCountInMemory());
        getCacheNotFound(session, cacheAccess);

        // Advance transaction time so it happens after region eviction
        testing.regionTimeService.advance(1, TimeUnit.MILLISECONDS);

        putFromLoad(cacheAccess, session);
        assertEquals(3, testing.region().getElementCountInMemory());

        getCacheFound(session, cacheAccess);
    }

    private static void getCacheNotFound(SharedSessionContractImplementor session, EntityDataAccess cacheAccess) {
        assertNull(cacheAccess.get(session, 1));
        assertNull(cacheAccess.get(session, 2));
        assertNull(cacheAccess.get(session, 3));
    }

    private static void getCacheFound(SharedSessionContractImplementor session, EntityDataAccess cacheAccess) {
        assertEquals("1-v1", cacheAccess.get(session, 1));
        assertEquals("2-v1", cacheAccess.get(session, 2));
        assertEquals("3-v1", cacheAccess.get(session, 3));
    }

    private static void putFromLoad(EntityDataAccess cacheAccess, SharedSessionContractImplementor session) {
        cacheAccess.putFromLoad(session, 1, "1-v1", null, false);
        cacheAccess.putFromLoad(session, 2, "2-v1", null, false);
        cacheAccess.putFromLoad(session, 3, "3-v1", null, false);
    }

}
