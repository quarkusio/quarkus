package io.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration class allowing to globally set TLS properties.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class TlsConfig {

    /**
     * Enable trusting all certificates. Disable by default.
     */
    @ConfigItem(defaultValue = "false")
    public boolean trustAll;

    @Override
    public String toString() {
        return "TlsConfig{" +
                "trustAll=" + trustAll +
                '}';
    }
}