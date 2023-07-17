package io.quarkus.it.bootstrap.config.extension;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BOOTSTRAP)
public class DummyConfig {

    /**
     * dummy name
     */
    public String name;

    /**
     * dummy times
     */
    @ConfigItem(defaultValue = "2")
    public Integer times;

    /**
     * dummy map
     */
    @ConfigItem(name = "map")
    public Map<String, MapConfig> map;

    @ConfigGroup
    public static class MapConfig {
        /**
         * dummy value
         */
        @ConfigItem(name = ConfigItem.PARENT)
        public String value;
    }
}
