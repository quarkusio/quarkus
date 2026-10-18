package io.quarkus.deployment.cmd;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DeployCommandDeclarationResultBuildItem extends SimpleBuildItem {
    private final List<String> commands;

    public DeployCommandDeclarationResultBuildItem(List<String> commands) {
        this.commands = commands;
    }

    public List<String> getCommands() {
        return commands;
    }
}
