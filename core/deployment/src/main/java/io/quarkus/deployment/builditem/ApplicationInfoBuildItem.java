package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

public final class ApplicationInfoBuildItem extends SimpleBuildItem {

    private final String name;
    private final String version;

    public ApplicationInfoBuildItem(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
