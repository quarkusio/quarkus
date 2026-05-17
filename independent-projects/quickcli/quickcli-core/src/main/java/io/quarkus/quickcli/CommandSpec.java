package io.quarkus.quickcli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete specification of a command, built at compile time by the annotation processor.
 * Contains all metadata needed to parse arguments and generate help without reflection.
 */
public final class CommandSpec {

    private final String name;
    private final String[] description;
    private final String[] version;
    private final boolean mixinStandardHelpOptions;
    private final String[] header;
    private final String[] footer;
    private final Class<?> commandClass;

    private final List<OptionSpec> options = new ArrayList<>();
    private final List<ParameterSpec> parameters = new ArrayList<>();
    private final Map<String, CommandSpec> subcommands = new LinkedHashMap<>();
    private CommandSpec parent;
    private CommandLine commandLine;
    private final UsageMessageSpec usageMessage = new UsageMessageSpec();
    private String[] aliases = {};
    private boolean hasUnmatchedField;
    private boolean hasSpecField;
    private Object commandInstance;
    private ScopeType scope = ScopeType.LOCAL;
    private Class<? extends VersionProvider> versionProviderClass;
    private final List<List<String>> exclusiveGroups = new ArrayList<>();

    public CommandSpec(String name, String[] description, String[] version,
                       boolean mixinStandardHelpOptions, String[] header,
                       String[] footer, Class<?> commandClass) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.mixinStandardHelpOptions = mixinStandardHelpOptions;
        this.header = header;
        this.footer = footer;
        this.commandClass = commandClass;
        // Sync with usage message
        usageMessage.header(header);
        usageMessage.description(description);
        usageMessage.footer(footer);
    }

    /**
     * Creates a bare CommandSpec for programmatic/dynamic command creation.
     * Used for plugin commands that are not annotation-processed.
     */
    public static CommandSpec create(String name, Class<?> commandClass) {
        return new CommandSpec(name, new String[0], new String[0], false,
                new String[0], new String[0], commandClass);
    }

    /** Get the pre-created command instance (for dynamic commands). */
    public Object commandInstance() {
        return commandInstance;
    }

    /** Set a pre-created command instance (for dynamic commands). */
    public void setCommandInstance(Object instance) {
        this.commandInstance = instance;
    }

    /** Returns the user object (command instance) associated with this spec.
     *  Picocli-compatible alias for commandInstance(). */
    public Object userObject() {
        return commandInstance;
    }

    public String name() {
        return name;
    }

    /** Returns all names: the primary name followed by aliases. */
    public String[] names() {
        if (aliases.length == 0) {
            return new String[] { name };
        }
        String[] result = new String[1 + aliases.length];
        result[0] = name;
        System.arraycopy(aliases, 0, result, 1, aliases.length);
        return result;
    }

    public String[] description() {
        return description;
    }

    public String[] version() {
        return version;
    }

    public boolean mixinStandardHelpOptions() {
        return mixinStandardHelpOptions;
    }

    public String[] header() {
        return header;
    }

    public String[] footer() {
        return footer;
    }

    public Class<?> commandClass() {
        return commandClass;
    }

    public void addOption(OptionSpec option) {
        options.add(option);
    }

    public List<OptionSpec> options() {
        return Collections.unmodifiableList(options);
    }

    public void addParameter(ParameterSpec parameter) {
        parameters.add(parameter);
    }

    public List<ParameterSpec> parameters() {
        return Collections.unmodifiableList(parameters);
    }

    public void addSubcommand(String name, CommandSpec subcommand) {
        subcommand.parent = this;
        subcommands.put(name, subcommand);
        propagateInheritedOptions(subcommand);
    }

    private void propagateInheritedOptions(CommandSpec subcommand) {
        CommandSpec current = this;
        while (current != null) {
            if (current.scope == ScopeType.INHERIT) {
                for (OptionSpec parentOption : current.options) {
                    if (subcommand.findOption(parentOption.longestName()) == null) {
                        subcommand.addOption(parentOption);
                    }
                }
                if (current.hasUnmatchedField && !subcommand.hasUnmatchedField) {
                    subcommand.setHasUnmatchedField(true);
                }
            }
            current = current.parent;
        }
        for (CommandSpec subSub : subcommand.subcommands.values()) {
            propagateInheritedOptions(subSub);
        }
    }

    public Map<String, CommandSpec> subcommands() {
        return subcommands; // mutable for runtime additions
    }

    public CommandSpec parent() {
        return parent;
    }

    /** Returns the root CommandSpec of the command hierarchy. */
    public CommandSpec root() {
        CommandSpec current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    /** Get the CommandLine that owns this spec. */
    public CommandLine commandLine() {
        return commandLine;
    }

    /** Set the CommandLine that owns this spec. */
    public void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    /** Get the usage message spec for help formatting. */
    public UsageMessageSpec usageMessage() {
        return usageMessage;
    }

    /** Exit code when help is requested (default 0). */
    public int exitCodeOnUsageHelp() {
        return 0;
    }

    /** Exit code when invalid input is provided (default 2). */
    public int exitCodeOnInvalidInput() {
        return 2;
    }

    /** Find an option by any of its names. */
    public OptionSpec findOption(String name) {
        for (OptionSpec option : options) {
            if (option.matches(name)) {
                return option;
            }
        }
        return null;
    }

    /** Get the full command name including parent chain (e.g., "app sub-cmd"). */
    public String qualifiedName() {
        if (parent != null) {
            return parent.qualifiedName() + " " + name;
        }
        return name;
    }

    public String[] aliases() {
        return aliases;
    }

    public void setAliases(String... aliases) {
        this.aliases = aliases;
    }

    public boolean hasUnmatchedField() {
        return hasUnmatchedField;
    }

    public void setHasUnmatchedField(boolean hasUnmatchedField) {
        this.hasUnmatchedField = hasUnmatchedField;
    }

    public boolean hasSpecField() {
        return hasSpecField;
    }

    public void setHasSpecField(boolean hasSpecField) {
        this.hasSpecField = hasSpecField;
    }

    public ScopeType scope() {
        return scope;
    }

    public void setScope(ScopeType scope) {
        this.scope = scope;
    }

    public void addExclusiveGroup(List<String> optionNames) {
        exclusiveGroups.add(optionNames);
    }

    public List<List<String>> exclusiveGroups() {
        return exclusiveGroups;
    }

    public Class<? extends VersionProvider> versionProviderClass() {
        return versionProviderClass;
    }

    public void setVersionProviderClass(Class<? extends VersionProvider> versionProviderClass) {
        this.versionProviderClass = versionProviderClass;
    }
}
