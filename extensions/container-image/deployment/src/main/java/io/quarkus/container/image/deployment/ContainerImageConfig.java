package io.quarkus.container.image.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class ContainerImageConfig {

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
     * The container registry to use
     */
    @ConfigItem
    public Optional<String> registry;

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

    /**
     * Controls what kind of execution is needed.
     * <ul>
     * <li>{@link io.quarkus.container.image.deployment.ContainerImageConfig.Execution#NONE} means that no container image will
     * be created</li>
     * <li>{@link io.quarkus.container.image.deployment.ContainerImageConfig.Execution#BUILD} will result in a container image
     * being created locally</li>
     * <li>{@link io.quarkus.container.image.deployment.ContainerImageConfig.Execution#PUSH} will result in a container image
     * being pushed to the specified registry</li>
     * </ul>
     */
    @ConfigItem(defaultValue = "none")
    public Execution execution;

    public enum Execution {
        NONE,
        BUILD,
        PUSH
    }
}
