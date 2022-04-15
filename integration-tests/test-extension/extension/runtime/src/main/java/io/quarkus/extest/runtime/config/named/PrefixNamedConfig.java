package io.quarkus.extest.runtime.config.named;

import java.util.Map;

import io.quarkus.extest.runtime.config.NestedConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(prefix = "my.prefix", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED, name = "named")
public class PrefixNamedConfig {
    /** */
    @ConfigItem
    public String prop;
    /** */
    @ConfigItem
    public Map<String, String> map;
    /** */
    @ConfigItem
    public NestedConfig nested;
}
