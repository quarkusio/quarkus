package io.quarkus.test.junit;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.common.PropertyTestUtil;

public class NonQuarkusTestLoggingEnabler implements BeforeAllCallback {

    private static boolean initialized;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!initialized) {
            initialized = true;
            if (!PropertyTestUtil.isLoggingSetupImminent()) {
                System.err.println("!!! NonQuarkusTestLoggingEnabler");
                try {
                    IntegrationTestUtil.activateLogging();
                } finally {
                    // release the config that was retrieved by above call so that tests that try to register their own config
                    // don't fail with:
                    // IllegalStateException: SRCFG00017: Configuration already registered for the given class loader
                    var configproviderResolver = ConfigProviderResolver.instance();
                    var config = configproviderResolver.getConfig();
                    if (config != null) { // probably never null, but be safe
                        configproviderResolver.releaseConfig(config);
                    }
                }
            }
        }
    }
}
