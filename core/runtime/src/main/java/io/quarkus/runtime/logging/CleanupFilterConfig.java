package io.quarkus.runtime.logging;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CleanupFilterConfig {
    /**
     * The message starts to match
     */
    @ConfigItem(defaultValue = "inherit")
    List<String> ifStartsWith;
}
