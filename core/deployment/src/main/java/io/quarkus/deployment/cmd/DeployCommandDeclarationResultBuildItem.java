package io.quarkus.deployment.cmd;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A {@link SimpleBuildItem} that represents the aggregate result of all
 * deployment actions.
 * This is used by build tools to report back the overall outcome of the
 * deployment process.
 */
public final class DeployCommandDeclarationResultBuildItem extends SimpleBuildItem {
    private final List<String> commands;

    public DeployCommandDeclarationResultBuildItem(List<String> commands) {
        this.commands = commands;
    }

    public List<String> getCommands() {
        return commands;
    }
}
