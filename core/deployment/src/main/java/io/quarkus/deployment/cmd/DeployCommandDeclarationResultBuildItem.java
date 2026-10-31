package io.quarkus.deployment.cmd;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains the names of extensions that declare support for {@code quarkus deploy}.
 * <p>
 * Build-tool plugins use this result to determine which deploy target to invoke.
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
