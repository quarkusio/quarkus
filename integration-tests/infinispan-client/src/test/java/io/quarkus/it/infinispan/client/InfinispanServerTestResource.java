package io.quarkus.it.infinispan.client;

import java.util.Collections;
import java.util.Map;

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
    public Map<String, String> start() {
        TestResourceTracker.setThreadTestName("InfinispanServer");
        EmbeddedCacheManager ecm = TestCacheManagerFactory.createCacheManager(
                new GlobalConfigurationBuilder().nonClusteredDefault().defaultCacheName("default"),
                new ConfigurationBuilder());
        ecm.defineConfiguration("magazine", new ConfigurationBuilder().build());
        // Client connects to a non default port
        hotRodServer = HotRodTestingUtil.startHotRodServer(ecm, 11232);
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (hotRodServer != null) {
            hotRodServer.stop();
        }
    }
}
