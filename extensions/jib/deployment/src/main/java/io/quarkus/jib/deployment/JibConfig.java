package io.quarkus.jib.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class JibConfig {

    /**
     * If enabled, a container image will be created using jib
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * If enabled, the image will be pushed to the registry
     */
    @ConfigItem(defaultValue = "false")
    public boolean push;

    /**
     * The group the container image will be part of
     */
    @ConfigItem
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
     * The container registry to use
     */
    @ConfigItem(defaultValue = "docker.io")
    public String registry;

    /**
     * The username to use to authenticate with the registry
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The password to use to authenticate with the registry
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Whether or not insecure registries are allowed
     */
    @ConfigItem(defaultValue = "false")
    public boolean insecure;
}
