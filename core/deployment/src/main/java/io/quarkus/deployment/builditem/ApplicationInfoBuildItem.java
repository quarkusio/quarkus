package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ApplicationInfoBuildItem extends SimpleBuildItem {

    public static final String UNSET_VALUE = "<<unset>>";

    private final String name;
    private final String version;

    public ApplicationInfoBuildItem(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name == null ? UNSET_VALUE : name;
    }

    public String getVersion() {
        return version == null ? UNSET_VALUE : version;
    }
}
