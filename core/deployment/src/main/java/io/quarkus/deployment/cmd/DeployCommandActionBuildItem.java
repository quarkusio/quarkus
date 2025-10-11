package io.quarkus.deployment.cmd;

import io.quarkus.builder.item.MultiBuildItem;
/**
 * A build item that indicates that a deploy command was executed.
 * This can be used to trigger additional actions after a deploy command.
 */
public final class DeployCommandActionBuildItem extends MultiBuildItem {
    
    /**
     * The name of the command that was executed (e.g., "deploy", "redeploy").
     */
    private final String commandName;

    /**
     * Indicates whether the command was successful.
     */
    private final boolean successful;

    /**
     * Creates a new DeployCommandActionBuildItem.
     *
     * @param commandName the name of the command that was executed
     * @param successful  whether the command was successful
     */
    public DeployCommandActionBuildItem(String commandName, boolean successful) {
        this.commandName = commandName;
        this.successful = successful;
    }

    /**
     * Gets the name of the command that was executed.
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * Indicates whether the command was successful.
     */
    public boolean isSuccessful() {
        return successful;
    }
}
