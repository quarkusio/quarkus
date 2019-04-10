package io.quarkus.it.camel.core;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.TestResourceTracker;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class InfinispanServerTestResource implements QuarkusTestResourceLifecycleManager {
    private HotRodServer hotRodServer;

    @Override
    public void start() {
        TestResourceTracker.setThreadTestName("InfinispanServer");
        EmbeddedCacheManager ecm = TestCacheManagerFactory.createCacheManager(
                new GlobalConfigurationBuilder().nonClusteredDefault().defaultCacheName("default"),
                new ConfigurationBuilder());
        // Client connects to a non default port
        hotRodServer = HotRodTestingUtil.startHotRodServer(ecm, 11232);
    }

    @Override
    public void stop() {
        if (hotRodServer != null) {
            hotRodServer.stop();
        }
    }
}
