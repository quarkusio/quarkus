package io.quarkus.extest.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(prefix = "another", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AnotherPrefixConfig {
    /** */
    @ConfigItem
    public String prop;
    /** */
    @ConfigItem
    public Map<String, String> map;
}
