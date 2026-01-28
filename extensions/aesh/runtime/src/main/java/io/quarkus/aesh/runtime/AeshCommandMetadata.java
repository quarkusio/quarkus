package io.quarkus.aesh.runtime;

import java.util.List;

/**
 * Mutable metadata describing a discovered aesh command.
 * <p>
 * This class is mutable so that it can be serialized by the Quarkus recorder mechanism.
 * At runtime, the recorder creates an immutable copy of the metadata list.
 *
 * @see AeshRecorder
 * @see AeshContext
 */
public class AeshCommandMetadata {

    private String className;
    private String commandName;
    private String description;
    private boolean groupCommand;
    private List<String> subCommandClassNames;
    private boolean topCommand;
    private boolean cliCommand;

    public AeshCommandMetadata() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isGroupCommand() {
        return groupCommand;
    }

    public void setGroupCommand(boolean groupCommand) {
        this.groupCommand = groupCommand;
    }

    public List<String> getSubCommandClassNames() {
        return subCommandClassNames;
    }

    public void setSubCommandClassNames(List<String> subCommandClassNames) {
        this.subCommandClassNames = subCommandClassNames;
    }

    public boolean isTopCommand() {
        return topCommand;
    }

    public void setTopCommand(boolean topCommand) {
        this.topCommand = topCommand;
    }

    public boolean isCliCommand() {
        return cliCommand;
    }

    public void setCliCommand(boolean cliCommand) {
        this.cliCommand = cliCommand;
    }
}
