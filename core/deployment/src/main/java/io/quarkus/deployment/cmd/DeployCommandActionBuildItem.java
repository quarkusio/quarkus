package io.quarkus.deployment.cmd;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that a deploy command action was executed successfully or not.  
 */
public final class DeployCommandActionBuildItem extends MultiBuildItem {
    /**
     * The name of the command that was executed (e.g., "azure-function").
     */
    private final String commandName;

    /**
     * Indicates whether the deploy action was successful.
     */
    private final boolean successful;

    /**
     * Creates a new DeployCommandActionBuildItem.
     */
    public DeployCommandActionBuildItem(String commandName, boolean successful) {
        this.commandName = commandName;
        this.successful = successful;
    }

    public String getCommandName() {
        return commandName;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
}
