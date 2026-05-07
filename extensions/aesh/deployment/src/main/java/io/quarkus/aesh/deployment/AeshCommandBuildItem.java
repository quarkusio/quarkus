package io.quarkus.aesh.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a CLI command class discovered during build-time index scanning.
 * <p>
 * Produced by the {@code discoverCommands} build step and consumed by subsequent steps
 * for mode detection, annotation transformation, bean registration, and recorder metadata.
 */
public final class AeshCommandBuildItem extends MultiBuildItem {

    private final String className;
    private final String commandName;
    private final String description;
    private final boolean groupCommand;
    private final List<String> subCommandClassNames;
    private final boolean topCommand;
    private final boolean cliCommand;

    public AeshCommandBuildItem(String className, String commandName, String description,
            boolean groupCommand, List<String> subCommandClassNames,
            boolean topCommand, boolean cliCommand) {
        this.className = className;
        this.commandName = commandName;
        this.description = description;
        this.groupCommand = groupCommand;
        this.subCommandClassNames = subCommandClassNames != null ? List.copyOf(subCommandClassNames) : List.of();
        this.topCommand = topCommand;
        this.cliCommand = cliCommand;
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

    public boolean isTopCommand() {
        return topCommand;
    }

    public boolean isCliCommand() {
        return cliCommand;
    }
}
