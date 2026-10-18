package io.quarkus.cli.plugin;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.runtime.util.StringUtil;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.ISetter;
import picocli.CommandLine.Model.PositionalParamSpec;

public class PluginCommandFactory {

    private final OutputOptionMixin output;

    //Testing
    protected PluginCommandFactory() {
        this(null);
    }

    public PluginCommandFactory(OutputOptionMixin output) {
        this.output = output;
    }

    private Optional<PluginCommand> createPluginCommand(Plugin plugin) {
        switch (plugin.getType()) {
            case maven:
                return plugin.getLocation().flatMap(PluginUtil::checkGACTV).map(g -> new JBangCommand(toGAVC(g), output));
            case java:
            case jar:
            case jbang:
                return plugin.getLocation().map(l -> new JBangCommand(l, output));
            case executable:
                return plugin.getLocation().map(l -> new ShellCommand(plugin.getName(), Paths.get(l), output));
            case extension:
                if (PluginUtil.checkGACTV(plugin.getLocation()).isPresent()) {
                    return plugin.getLocation().flatMap(PluginUtil::checkGACTV).map(g -> new JBangCommand(toGAVC(g), output));
                } else if (plugin.getLocation().filter(l -> l.endsWith(".jar")).isPresent()) {
                    return plugin.getLocation().map(l -> new JBangCommand(l, output));
                }
            default:
                throw new IllegalStateException("Unknown plugin type!");
        }
    }

    /**
     * Create a command for the specified plugin
     */
    public Optional<CommandSpec> createCommand(Plugin plugin) {
        return createPluginCommand(plugin).map(createCommandSpec(plugin.getDescription().orElse("")));
    }

    public Function<PluginCommand, CommandSpec> createCommandSpec(String description) {
        return command -> {
            CommandSpec spec = CommandSpec.wrapWithoutInspection(command);
            if (!StringUtil.isNullOrEmpty(description)) {
                spec.usageMessage().description(description);
            }
            spec.parser().unmatchedArgumentsAllowed(true);
            spec.parser().unmatchedOptionsArePositionalParams(true);
            spec.add(PositionalParamSpec.builder().type(String[].class).arity("0..*").description("Positional arguments")
                    .setter(new ISetter() {
                        @Override
                        public <T> T set(T value) throws Exception {
                            if (value == null) {
                                return value;
                            }
                            if (value instanceof String[]) {
                                String[] array = (String[]) value;
                                command.useArguments(Arrays.asList(array));
                            }
                            return value;
                        }
                    }).build());
            return spec;
        };
    }

    /**
     * Populate the plugin commands listed in the {@link PluginCatalog} to the {@link CommandLine}.
     *
     * @param cmd the CommandLine.
     * @param plugins the available plugins.
     * @param factory the factory use to create the commands.
     */
    public void populateCommands(CommandLine cmd, Map<String, Plugin> plugins) {
        plugins.entrySet().stream()
                .map(Map.Entry::getValue).forEach(plugin -> {
                    CommandLine current = cmd;
                    // The plugin is stripped from its prefix when added to the catalog.
                    // Let's added back to ensure it matches the CLI root command.
                    String name = cmd.getCommandName() + "-" + plugin.getName();
                    while (current != null && current.getCommandName() != null
                            && name.startsWith(current.getCommandName() + "-")) {
                        String remaining = name.substring(current.getCommandName().length() + 1);
                        name = remaining;
                        List<String> subcommandKeys = current.getSubcommands().keySet().stream()
                                .filter(k -> remaining.startsWith(k))
                                .collect(Collectors.toList());
                        Optional<String> matchedKey = subcommandKeys.stream().sorted(Comparator.comparingInt(String::length))
                                .findFirst();
                        if (!matchedKey.isPresent()) {
                            break;
                        }
                        current = current.getSubcommands().get(matchedKey.get());
                    }
                    //JBang aliases from remote catalogs are suffixed with '@<catalog name>'
                    //We keep the catalog in the name, so that we can call the command, but
                    //let's not use it in the subcommand name
                    name = name.contains("@") ? name.split("@")[0] : name;
                    final String commandName = name;
                    final CommandLine commandParent = current;
                    createCommand(plugin).ifPresent(command -> {
                        if (!commandParent.getSubcommands().containsKey(commandName)) {
                            commandParent.addSubcommand(commandName, command);
                        }
                    });
                });
    }

    private static String toGAVC(GACTV gactv) {
        return gactv.getGroupId() + ":" + gactv.getArtifactId() + ":" + gactv.getVersion()
                + (StringUtil.isNullOrEmpty(gactv.getClassifier()) ? "" : ":" + gactv.getClassifier());
    }
}
