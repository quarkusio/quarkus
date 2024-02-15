
package io.quarkus.infinispan.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class OverrideContainerConfigOnDevServicesTest {

    @Inject
    RemoteCacheManager cacheManager;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("server-config-override.xml"))
            .withConfigurationResource("dev-services-adds-server-config.properties");

    @Test
    public void remoteCacheManagerDefaultBeansAccessible() {
        assertThat(cacheManager.getCacheNames()).contains("my-local-cache");
    }
}
