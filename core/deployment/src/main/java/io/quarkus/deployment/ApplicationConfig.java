package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class ApplicationConfig {

    /**
     * The name of the application.
     * If not set, defaults to the name of the project.
     */
    @ConfigItem
    public String name;

    /**
     * The version of the application.
     * If not set, defaults to the version of the project
     */
    @ConfigItem
    public String version;
}
