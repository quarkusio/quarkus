package io.quarkus.test.config;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.extension.Extension;

import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.smallrye.config.SmallRyeConfig;

/**
 * A global JUnit extension that enables/sets up basic logging if logging has not already been set up.
 * <p/>
 * This is useful for getting log output from non-Quarkus tests (if executed separately or before the first Quarkus
 * test), but also for getting instant log output from {@code QuarkusTestResourceLifecycleManagers} etc.
 */
public class LoggingSetupExtension implements Extension {
    private static final String JUNIT_QUARKUS_ENABLE_BASIC_LOGGING = "junit.quarkus.enable-basic-logging";

    public LoggingSetupExtension() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        if (config.getOptionalValue(JUNIT_QUARKUS_ENABLE_BASIC_LOGGING, boolean.class).orElse(true)) {
            LoggingSetupRecorder.handleFailedStart();
        }
    }
}
