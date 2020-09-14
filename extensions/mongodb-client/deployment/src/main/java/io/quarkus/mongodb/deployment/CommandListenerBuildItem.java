package io.quarkus.mongodb.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class CommandListenerBuildItem extends SimpleBuildItem {

    private List<String> commandListenerClassNames;

    public CommandListenerBuildItem(List<String> commandListenerClassNames) {
        this.commandListenerClassNames = commandListenerClassNames;
    }

    public List<String> getCommandListenerClassNames() {
        return commandListenerClassNames;
    }
}
