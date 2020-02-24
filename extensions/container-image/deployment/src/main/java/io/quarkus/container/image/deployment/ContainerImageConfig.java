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
    @ConfigItem(defaultValue = "${quarkus.application.name:unset}")
    public Optional<String> name;

    /**
     * The tag of the container image. If not set defaults to the application version
     */
    @ConfigItem(defaultValue = "${quarkus.application.version:latest}")
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
     * Whether or not a image build will be performed.
     */
    @ConfigItem(defaultValue = "false")
    public boolean build;

    /**
     * Whether or not an image push will be performed.
     */
    @ConfigItem(defaultValue = "false")
    public boolean push;
}
