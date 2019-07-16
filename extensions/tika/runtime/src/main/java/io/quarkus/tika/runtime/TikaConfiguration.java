package io.quarkus.tika.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Tika parser configuration
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class TikaConfiguration {
    /**
     * The path to the tika-config.xml
     */
    @ConfigItem(defaultValue = "tika-config.xml")
    public String tikaConfigPath;

    /**
     * Controls how the content of the embedded documents is parsed.
     * By default it is appended to the master document content.
     * Setting this property to false makes the content of each of the embedded documents
     * available separately.
     */
    @ConfigItem(defaultValue = "true")
    public boolean appendEmbeddedContent;
}
