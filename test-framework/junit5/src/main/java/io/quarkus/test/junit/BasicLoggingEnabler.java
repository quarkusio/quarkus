package io.quarkus.test.junit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.logging.InitialConfigurator;

/**
 * A (global) JUnit callback that enables/sets up basic logging if logging has not already been set up.
 * <p/>
 * This is useful for getting log output from non-Quarkus tests (if executed separately or before the first Quarkus test),
 * but also for getting instant log output from {@code QuarkusTestResourceLifecycleManagers} etc.
 * <p/>
 * This callback can be disabled via {@link #CFGKEY_ENABLED} in {@code junit-platform.properties} or via system property.
 */
public class BasicLoggingEnabler implements BeforeAllCallback {

    private static final String CFGKEY_ENABLED = "junit.quarkus.enable-basic-logging";
    private static Boolean enabled;

    // to speed things up a little, eager async loading of the config that will be looked up in LoggingSetupRecorder
    // downside: doesn't obey CFGKEY_ENABLED, but that should be bearable
    static {
        // e.g. continuous testing has everything set up already (DELAYED_HANDLER is active)
        if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
            new Thread(() -> ConfigProvider.getConfig()).start();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (enabled == null) {
            enabled = context.getConfigurationParameter(CFGKEY_ENABLED).map(Boolean::valueOf).orElse(Boolean.TRUE);
        }
        if (!enabled || InitialConfigurator.DELAYED_HANDLER.isActivated()) {
            return;
        }
        try {
            IntegrationTestUtil.activateLogging();
        } finally {
            // release the config that was retrieved by above call so that tests that try to register their own config
            // don't fail with:
            // "IllegalStateException: SRCFG00017: Configuration already registered for the given class loader"
            // also, a possible recreation of basically the same config for a later test class will consume far less time
            var configProviderResolver = ConfigProviderResolver.instance();
            var config = configProviderResolver.getConfig();
            if (config != null) { // probably never null, but be safe
                configProviderResolver.releaseConfig(config);
            }
        }
    }
}
