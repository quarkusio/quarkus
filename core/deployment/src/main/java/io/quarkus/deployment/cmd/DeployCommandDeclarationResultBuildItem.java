package io.quarkus.deployment.cmd;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item produced by {@link DeployCommandProcessor} to declare the deploy
 * commands that should be registered.
 * <p>
 * This build item is used by build tools (like Maven and Gradle) to discover
 * which extensions
 * have registered themselves as capable of performing a deployment.
 * <p>
 * The list of commands is aggregated from all
 * {@link DeployCommandDeclarationBuildItem}s.
 */
public final class DeployCommandDeclarationResultBuildItem extends SimpleBuildItem {
    /**
     * @param commands the list of commands that should be registered as deploy commands
     */
    private final List<String> commands;

    /**
     * @param commands the list of commands that should be registered as deploy commands
     */
    public DeployCommandDeclarationResultBuildItem(List<String> commands) {
        this.commands = commands;
    }

    /**
     * @return the list of commands that should be registered as deploy commands
     */
    public List<String> getCommands() {
        return commands;
    }
}
