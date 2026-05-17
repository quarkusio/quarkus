package io.quarkus.quickcli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of parsing command-line arguments. Contains matched options, parameters,
 * unmatched arguments, and any subcommand parse results.
 */
public final class ParseResult {

    private final CommandSpec commandSpec;
    private final Map<String, List<String>> optionValues = new LinkedHashMap<>();
    private final List<String> positionalValues = new ArrayList<>();
    private final List<String> unmatchedArgs = new ArrayList<>();
    private ParseResult subcommandResult;
    private boolean helpRequested;
    private boolean versionRequested;
    private final List<String> originalArgs;

    public ParseResult(CommandSpec commandSpec, List<String> originalArgs) {
        this.commandSpec = commandSpec;
        this.originalArgs = originalArgs;
    }

    public CommandSpec commandSpec() {
        return commandSpec;
    }

    public void addOptionValue(String name, String value) {
        optionValues.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    public boolean hasOption(String name) {
        return optionValues.containsKey(name);
    }

    public String getOptionValue(String name) {
        List<String> values = optionValues.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    public List<String> getOptionValues(String name) {
        return optionValues.getOrDefault(name, List.of());
    }

    /** Returns the matched option value for the given option name (checking all aliases). */
    public OptionSpec matchedOption(String name) {
        // Find the OptionSpec that matches this name
        for (OptionSpec option : commandSpec.options()) {
            if (option.matches(name) || option.matches("--" + name)) {
                if (hasOptionBySpec(option)) {
                    return option;
                }
            }
        }
        return null;
    }

    public void addPositionalValue(String value) {
        positionalValues.add(value);
    }

    public List<String> positionalValues() {
        return positionalValues;
    }

    public String getPositionalValue(int index) {
        return index < positionalValues.size() ? positionalValues.get(index) : null;
    }

    /** Returns the subcommand parse result (picocli-compatible alias for subcommandResult). */
    public ParseResult subcommand() {
        return subcommandResult;
    }

    public ParseResult subcommandResult() {
        return subcommandResult;
    }

    public void setSubcommandResult(ParseResult subcommandResult) {
        this.subcommandResult = subcommandResult;
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public void setHelpRequested(boolean helpRequested) {
        this.helpRequested = helpRequested;
    }

    public boolean isVersionRequested() {
        return versionRequested;
    }

    public void setVersionRequested(boolean versionRequested) {
        this.versionRequested = versionRequested;
    }

    public List<String> originalArgs() {
        return originalArgs;
    }

    /** Returns unmatched arguments (only populated when @Unmatched is present). */
    public List<String> unmatched() {
        return unmatchedArgs;
    }

    public void addUnmatched(String arg) {
        unmatchedArgs.add(arg);
    }

    /** Find the option value, checking all known aliases for the option. */
    public String resolveOptionValue(OptionSpec option) {
        for (String name : option.names()) {
            String value = getOptionValue(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /** Check if any alias for the given option was specified. */
    public boolean hasOptionBySpec(OptionSpec option) {
        for (String name : option.names()) {
            if (hasOption(name)) {
                return true;
            }
        }
        return false;
    }
}
