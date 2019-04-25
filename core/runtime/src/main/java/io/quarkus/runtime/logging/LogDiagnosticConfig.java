package io.quarkus.runtime.logging;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(name = "log.diagnostic", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class LogDiagnosticConfig {
    /**
     * Enable diagnostic output for logging.
     */
    @ConfigItem
    public boolean enable;
}
