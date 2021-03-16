package io.quarkus.it.infinispan.client;

import java.util.Collections;
import java.util.Map;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.test.TestResourceTracker;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class InfinispanServerTestResource implements QuarkusTestResourceLifecycleManager {

    private static final char[] PASSWORD = "changeit".toCharArray();

    private HotRodServer hotRodServer;

    @Override
    public Map<String, String> start() {
        TestResourceTracker.setThreadTestName("InfinispanServer");
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE);
        EmbeddedCacheManager ecm = TestCacheManagerFactory.createCacheManager(
                new GlobalConfigurationBuilder().nonClusteredDefault().defaultCacheName("default"),
                configurationBuilder);

        ecm.defineConfiguration("magazine", configurationBuilder.build());

        // Client connects to a non default port
        final HotRodServerConfigurationBuilder hotRodServerConfigurationBuilder = new HotRodServerConfigurationBuilder();
        hotRodServerConfigurationBuilder
                .ssl()
                .enabled(true)
                .keyStoreFileName("src/main/resources/server.p12")
                .keyStorePassword(PASSWORD)
                .keyStoreType("PKCS12")
                .requireClientAuth(false)
                .protocol("TLSv1.2");

        hotRodServer = HotRodTestingUtil.startHotRodServer(ecm, 11232, hotRodServerConfigurationBuilder);
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (hotRodServer != null) {
            hotRodServer.stop();
        }
    }

}
