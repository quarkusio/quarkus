package io.quarkus.tck.restclient;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.rest.client.tck.InvokeWithJsonPProviderTest;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.SmallRyeConfig;

/**
 *
 */
public class CustomInvokeWithJsonPProviderTest extends InvokeWithJsonPProviderTest {
    @BeforeTest
    public void setupClient() throws Exception {
        SmallRyeConfig config = ConfigUtils.configBuilder(true).build();
        QuarkusConfigFactory.setConfig(config);
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            Config old = cpr.getConfig();
            if (old != config) {
                cpr.releaseConfig(old);
            }
        } catch (IllegalStateException ignored) {
        }
        super.setupClient();
    }

    @AfterTest
    public void tearDownClient() {
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            Config old = cpr.getConfig();
            cpr.releaseConfig(old);
        } catch (IllegalStateException ignored) {
        }
    }
}
