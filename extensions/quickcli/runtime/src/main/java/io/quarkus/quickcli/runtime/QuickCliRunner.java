package io.quarkus.quickcli.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;

import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.OptionSpec;
import io.quarkus.quickcli.ParseResult;
import io.quarkus.runtime.QuarkusApplication;

@Dependent
public class QuickCliRunner implements QuarkusApplication {

    private final CommandLine commandLine;
    private final Event<ParseResult> parseResultEvent;

    public QuickCliRunner(CommandLine commandLine, Event<ParseResult> parseResultEvent) {
        this.commandLine = commandLine;
        this.parseResultEvent = parseResultEvent;
    }

    @Override
    public int run(String... args) throws Exception {
        // Set a custom execution strategy that fires CDI events before execution
        commandLine.setExecutionStrategy((parseResult, cmd) -> {
            // Populate SmallRye Config with parsed CLI options before command execution
            QuickCliConfigSource.setProperties(extractConfigProperties(parseResult));
            parseResultEvent.fire(parseResult);
            return new CommandLine.RunLastStrategy().execute(parseResult, cmd);
        });

        return commandLine.execute(args);
    }

    /**
     * Extracts parsed option values as config properties from the entire parse
     * result chain (including subcommands). Option names are mapped by stripping
     * leading dashes from the longest name (e.g. {@code --server.port} becomes
     * {@code server.port}).
     */
    static Map<String, String> extractConfigProperties(ParseResult parseResult) {
        Map<String, String> properties = new LinkedHashMap<>();
        collectOptions(parseResult, properties);
        return properties;
    }

    private static void collectOptions(ParseResult parseResult, Map<String, String> properties) {
        CommandSpec spec = parseResult.commandSpec();
        for (OptionSpec option : spec.options()) {
            if (option.isUsageHelp() || option.isVersionHelp()) {
                continue;
            }
            if (!parseResult.hasOptionBySpec(option)) {
                continue;
            }
            String configKey = toConfigKey(option.longestName());
            List<String> values = parseResult.getOptionValues(option.longestName());
            if (!values.isEmpty()) {
                // For multi-value options, join with comma (SmallRye Config convention)
                properties.put(configKey, String.join(",", values));
            }
        }
        if (parseResult.subcommandResult() != null) {
            collectOptions(parseResult.subcommandResult(), properties);
        }
    }

    private static String toConfigKey(String optionName) {
        // Strip leading dashes: --server.port -> server.port, -v -> v
        if (optionName.startsWith("--")) {
            return optionName.substring(2);
        } else if (optionName.startsWith("-")) {
            return optionName.substring(1);
        }
        return optionName;
    }
}
