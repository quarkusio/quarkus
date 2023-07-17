package io.quarkus.deployment.cmd;

import io.quarkus.builder.item.MultiBuildItem;

public final class DeployCommandActionBuildItem extends MultiBuildItem {
    private final String commandName;
    private final boolean successful;

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
