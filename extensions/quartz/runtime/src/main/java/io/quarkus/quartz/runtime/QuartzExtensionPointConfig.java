package io.quarkus.quartz.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class QuartzExtensionPointConfig {
    /**
     * Class name for the configuration.
     */
    @ConfigItem(name = "class")
    public String clazz;

    /**
     * The properties passed to the class.
     */
    @ConfigItem
    @ConfigDocMapKey("property-name")
    public Map<String, String> properties;
}
