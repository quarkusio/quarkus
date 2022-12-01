package io.quarkus.hibernate.orm;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.TimestampsCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

public class JPACacheDisabledTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Address.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.second-level-caching-enabled", "false");

    @Inject
    Session session;

    @Test
    @Transactional
    public void testNTransaction() {
        CacheImplementor cache = (CacheImplementor) session.getSessionFactory().getCache();
        TimestampsCache timestampsCache = cache.getTimestampsCache();
        Assertions.assertNull(timestampsCache);
    }

}
