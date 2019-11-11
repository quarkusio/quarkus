package io.quarkus.deployment.builditem;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

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
