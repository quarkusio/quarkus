package io.quarkus.qute.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class QuteConfig {

    /**
     * The set of suffixes used when attempting to locate a template.
     */
    @ConfigItem(defaultValue = "html,txt")
    public List<String> suffixes;

}
