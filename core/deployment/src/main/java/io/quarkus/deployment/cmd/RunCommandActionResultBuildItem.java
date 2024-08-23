package io.quarkus.deployment.cmd;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class RunCommandActionResultBuildItem extends SimpleBuildItem {
    private final List<RunCommandActionBuildItem> commands;

    public RunCommandActionResultBuildItem(List<RunCommandActionBuildItem> commands) {
        this.commands = commands;
    }

    public List<RunCommandActionBuildItem> getCommands() {
        return commands;
    }
}
