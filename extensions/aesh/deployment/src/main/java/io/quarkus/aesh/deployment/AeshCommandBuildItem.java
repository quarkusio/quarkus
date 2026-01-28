package io.quarkus.aesh.deployment;

import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a CLI command class discovered during build-time index scanning.
 * <p>
 * Produced by the {@code discoverCommands} build step and consumed by subsequent steps
 * for mode detection, bean registration, and recorder metadata.
 */
public final class AeshCommandBuildItem extends MultiBuildItem {

    private final String className;
    private final String commandName;
    private final String description;
    private final boolean groupCommand;
    private final List<String> subCommandClassNames;

    /**
     * @param className the fully qualified class name of the command, must not be {@code null}
     * @param commandName the command name from the annotation, must not be {@code null}
     * @param description the command description, may be {@code null}
     * @param groupCommand whether this is a group command with sub-commands
     * @param subCommandClassNames the sub-command class names, may be {@code null} (treated as empty)
     */
    public AeshCommandBuildItem(String className, String commandName, String description,
            boolean groupCommand, List<String> subCommandClassNames) {
        this.className = Objects.requireNonNull(className, "className must not be null");
        this.commandName = Objects.requireNonNull(commandName, "commandName must not be null");
        this.description = description;
        this.groupCommand = groupCommand;
        this.subCommandClassNames = subCommandClassNames != null ? List.copyOf(subCommandClassNames) : List.of();
    }

    public String getClassName() {
        return className;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isGroupCommand() {
        return groupCommand;
    }

    public List<String> getSubCommandClassNames() {
        return subCommandClassNames;
    }
}
