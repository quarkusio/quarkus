package io.quarkus.deployment.dev;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class DevConfig {

    /**
     * Whether or not Quarkus should disable it's ability to not do a full restart
     * when changes to classes are compatible with JVM instrumentation.
     * 
     * If this is set to false, Quarkus will always restart on changes and never perform class redefinition.
     */
    @ConfigItem(defaultValue = "true")
    boolean instrumentation;

}
