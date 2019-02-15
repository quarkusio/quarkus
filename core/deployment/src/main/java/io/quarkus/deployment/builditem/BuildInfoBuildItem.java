package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.deployment.BuildInfo;

public final class BuildInfoBuildItem extends SimpleBuildItem {

    private final BuildInfo buildInfo;

    public BuildInfoBuildItem(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
