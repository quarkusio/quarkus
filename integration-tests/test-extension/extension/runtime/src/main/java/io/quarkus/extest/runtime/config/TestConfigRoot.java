package io.quarkus.extest.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "root", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class TestConfigRoot {
    /**
     * resource location of the DSAPublicKey encoded bytes
     */
    @ConfigItem
    public String dsaKeyLocation;

    /**
     * Should the TestProcessor#checkConfig method validate the buildTimeConfig
     */
    @ConfigItem(defaultValue = "false")
    public boolean validateBuildConfig;
}
