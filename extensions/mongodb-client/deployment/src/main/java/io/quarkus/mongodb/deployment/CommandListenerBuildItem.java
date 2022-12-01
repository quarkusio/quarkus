package io.quarkus.mongodb.deployment;

import java.util.List;

import com.mongodb.event.CommandListener;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Register additional {@link CommandListener}s for the MongoDB clients.
 */
public final class CommandListenerBuildItem extends SimpleBuildItem {

    private final List<String> commandListenerClassNames;

    public CommandListenerBuildItem(List<String> commandListenerClassNames) {
        this.commandListenerClassNames = commandListenerClassNames;
    }

    public List<String> getCommandListenerClassNames() {
        return commandListenerClassNames;
    }
}
