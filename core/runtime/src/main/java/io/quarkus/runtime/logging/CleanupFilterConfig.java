package io.quarkus.runtime.logging;

import java.util.List;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CleanupFilterConfig {
    /**
     * The message prefix to match
     */
    @ConfigItem(defaultValue = "inherit")
    public List<String> ifStartsWith;

    /**
     * The new log level for the filtered message. Defaults to DEBUG.
     */
    @ConfigItem(defaultValue = "DEBUG")
    public Level targetLevel;
}
