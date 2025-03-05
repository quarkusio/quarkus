package io.quarkus.container.image.deployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.container-image")
public interface ContainerImageConfig {

    /**
     * The group the container image will be part of
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> group(); //used only by ContainerImageProcessor, use ContainerImageInfoBuildItem instead

    /**
     * The name of the container image. If not set defaults to the application name
     */
    @WithDefault("${quarkus.application.name:unset}")
    @WithConverter(TrimmedStringConverter.class)
    String name(); //used only by ContainerImageProcessor, use ContainerImageInfoBuildItem instead

    /**
     * The tag of the container image. If not set defaults to the application version
     */
    @WithDefault("${quarkus.application.version:latest}")
    String tag(); //used only by ContainerImageProcessor, use ContainerImageInfoBuildItem instead

    /**
     * Additional tags of the container image.
     */
    Optional<List<String>> additionalTags();

    /**
     * Custom labels to add to the generated image.
     */
    @ConfigDocMapKey("label-name")
    Map<String, String> labels();

    /**
     * The container registry to use
     */
    Optional<String> registry();

    /**
     * Represents the entire image string.
     * If set, then {@code group}, {@code name}, {@code registry}, {@code tags}, {@code additionalTags}
     * are ignored
     */
    Optional<String> image();

    /**
     * The username to use to authenticate with the registry where the built image will be pushed
     */
    Optional<String> username();

    /**
     * The password to use to authenticate with the registry where the built image will be pushed
     */
    Optional<String> password();

    /**
     * Whether or not insecure registries are allowed
     */
    @WithDefault("false")
    boolean insecure();

    /**
     * Whether or not a image build will be performed.
     */
    Optional<Boolean> build();

    /**
     * Whether or not an image push will be performed.
     */
    Optional<Boolean> push();

    /**
     * The name of the container image extension to use (e.g. docker, podman, jib, s2i).
     * The option will be used in case multiple extensions are present.
     */
    Optional<String> builder();

    default boolean isBuildExplicitlyEnabled() {
        return build().isPresent() && build().get();
    }

    default boolean isBuildExplicitlyDisabled() {
        return build().isPresent() && !build().get();
    }

    default boolean isPushExplicitlyEnabled() {
        return push().isPresent() && push().get();
    }

    default boolean isPushExplicitlyDisabled() {
        return push().isPresent() && !push().get();
    }
}
