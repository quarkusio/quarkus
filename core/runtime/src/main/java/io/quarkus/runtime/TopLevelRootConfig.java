package io.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.profile=someProfile
 *
 * TODO refactor code to actually use these values
 */
@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.RUN_TIME)
public class TopLevelRootConfig {

    /**
     * Profile that will be active when Quarkus launches
     */
    @ConfigItem(defaultValue = "prod")
    String profile;
}
