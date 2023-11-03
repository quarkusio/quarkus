package io.quarkus.deployment.cmd;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DeployCommandActionResultBuildItem extends SimpleBuildItem {
    private final List<DeployCommandActionBuildItem> commands;

    public DeployCommandActionResultBuildItem(List<DeployCommandActionBuildItem> commands) {
        this.commands = commands;
    }

    public List<DeployCommandActionBuildItem> getCommands() {
        return commands;
    }
}
