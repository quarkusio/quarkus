package io.quarkus.deployment.builditem;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This build item holds essential metadata about the application, specifically its name and version.
 * The values can be configured using the following properties:
 * <ul>
 * <li>{@code quarkus.application.name} - Sets the application name</li>
 * <li>{@code quarkus.application.version} - Sets the application version</li>
 * </ul>
 *
 * This configuration is intended to be used by extensions that require application metadata,
 * such as the kubernetes extension.
 */
public final class ApplicationInfoBuildItem extends SimpleBuildItem {

    public static final String UNSET_VALUE = "<<unset>>";

    private final String name;
    private final String version;

    public ApplicationInfoBuildItem(Optional<String> name, Optional<String> version) {
        this.name = name.orElse(UNSET_VALUE);
        this.version = version.orElse(UNSET_VALUE);
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
