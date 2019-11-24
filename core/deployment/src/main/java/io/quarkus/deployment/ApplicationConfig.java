package io.quarkus.deployment;

import java.util.Optional;

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
    public Optional<String> name;

    /**
     * The version of the application.
     * If not set, defaults to the version of the project
     */
    @ConfigItem
    public Optional<String> version;
}
