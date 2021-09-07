package io.quarkus.container.image.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class ContainerImageConfig {

    /**
     * The group the container image will be part of
     */
    @ConfigItem(defaultValue = "${user.name}")
    public Optional<String> group;

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
     * Additional tags of the container image.
     */
    @ConfigItem
    public Optional<List<String>> additionalTags;

    /**
     * Custom labels to add to the generated image.
     */
    @ConfigItem
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
    public boolean build;

    /**
     * Whether or not an image push will be performed.
     */
    @ConfigItem
    public boolean push;

    /**
     * The name of the container image extension to use (e.g. docker, jib, s2i).
     * The option will be used in case multiple extensions are present.
     */
    @ConfigItem
    public Optional<String> builder;

    /**
     * Since user.name which is default value can be uppercase and uppercase values are not allowed
     * in the repository part of image references, we need to make the username lowercase.
     * If spaces exist in the user name, we replace them with the dash character.
     *
     * We purposely don't change the value of an explicitly set group.
     */
    public Optional<String> getEffectiveGroup() {
        if (group.isPresent()) {
            String originalGroup = group.get();
            if (originalGroup.equals(System.getProperty("user.name"))) {
                return Optional.of(originalGroup.toLowerCase().replace(' ', '-'));
            }
        }
        return group;
    }
}
