package io.quarkus.quartz.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping
public interface QuartzExtensionPointConfig {
    /**
     * Class name for the configuration.
     */
    @WithName("class")
    String clazz();

    /**
     * The properties passed to the class.
     */
    @ConfigDocMapKey("property-key")
    Map<String, String> properties();
}
