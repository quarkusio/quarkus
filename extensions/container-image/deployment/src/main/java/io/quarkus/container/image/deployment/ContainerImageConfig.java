package io.quarkus.container.image.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class ContainerImageConfig {

    /**
     * The container registry to use
     */
    @ConfigItem
    public Optional<String> registry;

    /**
     * The group the container image will be part of
     */
    @ConfigItem(defaultValue = "${user.name}")
    public String group;

    /**
     * The name of the container image. If not set defaults to the application name
     */
    @ConfigItem
    public Optional<String> name;

    /**
     * The tag of the container image. If not set defaults to the application version
     */
    @ConfigItem
    public Optional<String> tag;

    /**
     * Flag that specifies if container build is enabled
     */
    @ConfigItem(defaultValue = "false")
    public boolean build;

    /**
     * Flag that specifies if container deploy is enabled
     */
    @ConfigItem(defaultValue = "false")
    public boolean push;
}
