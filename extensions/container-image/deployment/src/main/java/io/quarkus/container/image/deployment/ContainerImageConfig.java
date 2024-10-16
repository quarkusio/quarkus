package io.quarkus.container.image.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

@ConfigRoot
public class ContainerImageConfig {

    /**
     * The group the container image will be part of
     */
    @ConfigItem
    @ConvertWith(TrimmedStringConverter.class)
    Optional<String> group; //used only by ContainerImageProcessor, use ContainerImageInfoBuildItem instead

    /**
     * The name of the container image. If not set defaults to the application name
     */
    @ConfigItem(defaultValue = "${quarkus.application.name:unset}")
    @ConvertWith(TrimmedStringConverter.class)
    String name; //used only by ContainerImageProcessor, use ContainerImageInfoBuildItem instead

    /**
     * The tag of the container image. If not set defaults to the application version
     */
    @ConfigItem(defaultValue = "${quarkus.application.version:latest}")
    Optional<String> tag; //used only by ContainerImageProcessor, use ContainerImageInfoBuildItem instead

    /**
     * Additional tags of the container image.
     */
    @ConfigItem
    public Optional<List<String>> additionalTags;

    /**
     * Custom labels to add to the generated image.
     */
    @ConfigItem
    @ConfigDocMapKey("label-name")
    public Map<String, String> labels;

    /**
     * The container registry to use
     */
    @ConfigItem
    public Optional<String> registry;

    /**
     * Represents the entire image string.
     * If set, then {@code group}, {@code name}, {@code registry}, {@code tags}, {@code additionalTags}
     * are ignored
     */
    @ConfigItem
    public Optional<String> image;

    /**
     * The username to use to authenticate with the registry where the built image will be pushed
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The password to use to authenticate with the registry where the built image will be pushed
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Whether or not insecure registries are allowed
     */
    @ConfigItem
    public boolean insecure;

    /**
     * Whether or not a image build will be performed.
     */
    @ConfigItem
    public Optional<Boolean> build;

    /**
     * Whether or not an image push will be performed.
     */
    @ConfigItem
    public Optional<Boolean> push;

    /**
     * The name of the container image extension to use (e.g. docker, podman, jib, s2i).
     * The option will be used in case multiple extensions are present.
     */
    @ConfigItem
    public Optional<String> builder;

    public boolean isBuildExplicitlyEnabled() {
        return build.isPresent() && build.get();
    }

    public boolean isBuildExplicitlyDisabled() {
        return build.isPresent() && !build.get();
    }

    public boolean isPushExplicitlyEnabled() {
        return push.isPresent() && push.get();
    }

    public boolean isPushExplicitlyDisabled() {
        return push.isPresent() && !push.get();
    }
}
