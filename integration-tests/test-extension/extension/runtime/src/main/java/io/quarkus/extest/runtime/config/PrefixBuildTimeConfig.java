package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(prefix = "my.prefix.bt", phase = ConfigPhase.BUILD_TIME, name = "")
public class PrefixBuildTimeConfig {
    /** */
    @ConfigItem
    public String prop;
    /** */
    @ConfigItem
    public Map<String, String> map;
    /** */
    @ConfigItem
    public NestedConfig nested;

    static {
        System.setProperty("my.prefix.bt.map.prop", "1234");
    }
}
