package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.infinispan.protean.hibernate.cache.Eventually.eventually;
import static org.junit.Assert.assertEquals;

public class CacheRegionMaxSizeTest {

    @Test
    public void testMaxSize() {
        final Map configValues = new HashMap();
        configValues.put("hibernate.cache.com.acme.EntityB.memory.size", "3");

        CacheRegionTesting testing = CacheRegionTesting.cacheRegion("com.acme.EntityB", configValues);
        SharedSessionContractImplementor session = testing.session;

        EntityDataAccess cacheAccess = testing.entityCache(AccessType.READ_WRITE);
        putFromLoad(cacheAccess, session);
        assertEquals(3, testing.region().getElementCountInMemory());
        getCacheFound(session, cacheAccess);

        // Add another entity that makes the cache exceed its size
        cacheAccess.putFromLoad(session, 4, "4-v1", null, false);

        eventually(() -> assertEquals(3, testing.region().getElementCountInMemory()));
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
