package io.quarkus.cli;

import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.OutputProvider;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.TargetQuarkusPlatformGroup;
import io.quarkus.cli.common.VersionHelper;
import io.quarkus.cli.common.registry.RegistryClientMixin;
import io.quarkus.cli.plugin.Plugin;
import io.quarkus.cli.plugin.PluginCommandFactory;
import io.quarkus.cli.plugin.PluginListItem;
import io.quarkus.cli.plugin.PluginListTable;
import io.quarkus.cli.plugin.PluginManager;
import io.quarkus.cli.plugin.PluginManagerSettings;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.utils.Prompt;
import io.quarkus.runtime.QuarkusApplication;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Help;
import picocli.CommandLine.IHelpSectionRenderer;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.UsageMessageSpec;
import picocli.CommandLine.MutuallyExclusiveArgsException;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.UnmatchedArgumentException;

@CommandLine.Command(name = "quarkus", subcommands = {
        Create.class,
        Build.class,
        Dev.class,
        Run.class,
        Test.class,
        Config.class,
        ProjectExtensions.class,
        Image.class,
        Deploy.class,
        Registry.class,
        Info.class,
        Update.class,
        Version.class,
        CliPlugins.class,
        Completion.class }, scope = ScopeType.INHERIT, sortOptions = false, showDefaultValues = true, versionProvider = Version.class, subcommandsRepeatable = false, mixinStandardHelpOptions = false, commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "Options:%n", headerHeading = "%n", parameterListHeading = "%n")
public class QuarkusCli implements QuarkusApplication, OutputProvider, Callable<Integer> {
    static {
        System.setProperty("picocli.endofoptions.description", "End of command line options.");
    }

    private static final Set<String> CATCH_ALL_COMMANDS = Set.of("create", "image", "extension", "ext", "plugin", "plug");

    @Inject
    CommandLine.IFactory factory;

    @CommandLine.Mixin
    protected RegistryClientMixin registryClient;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Option(names = { "-v",
            "--version" }, versionHelp = true, description = "Print CLI version information and exit.")
    public boolean showVersion;

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.ArgGroup(exclusive = false, validate = false)
    protected PropertiesOptions propertiesOptions = new PropertiesOptions();

    public OutputOptionMixin getOutput() {
        return output;
    }

    @Override
    public int run(String... args) throws Exception {
        CommandLine cmd = factory == null ? new CommandLine(this) : new CommandLine(this, factory);
        cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, new SubCommandListRenderer());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());

        //When running tests the cli should not prompt for user input.
        boolean interactiveMode = Arrays.stream(args).noneMatch(arg -> arg.equals("--cli-test"));
        Optional<String> testDir = Arrays.stream(args).dropWhile(arg -> !arg.equals("--cli-test-dir")).skip(1).findFirst();
        boolean noCommand = args.length == 0 || args[0].startsWith("-");
        boolean helpCommand = Arrays.stream(args).anyMatch(arg -> arg.equals("--help"));
        boolean pluginCommand = args.length >= 1 && (args[0].equals("plug") || args[0].equals("plugin"));
        boolean pluginSyncCommand = pluginCommand && args.length >= 2 && args[1].equals("sync");

        try {
            Optional<String> missingCommand = findMissingCommand(cmd, args);

            boolean existingCommand = missingCommand.isEmpty();
            // If the command already exists and is not a help command (that lists subcommands) or plugin command, then just execute
            // without dealing with plugins.
            // The reason that we check if its a plugin command is that plugin commands need PluginManager initialization.
            if (existingCommand && !noCommand && !helpCommand && !pluginCommand) {
                return cmd.execute(args);
            }
            PluginCommandFactory pluginCommandFactory = new PluginCommandFactory(output);
            PluginManager pluginManager = pluginManager(output, testDir, interactiveMode);
            if (!pluginSyncCommand) { // Let`s not sync before the actual command
                pluginManager.syncIfNeeded();
            }
            Map<String, Plugin> plugins = new HashMap<>(pluginManager.getInstalledPlugins());
            pluginCommandFactory.populateCommands(cmd, plugins);
            missingCommand.filter(m -> !plugins.containsKey(m)).ifPresent(m -> {
                try {
                    output.info("Unable to match command `%s`, looking for available plugins...", m);
                    Map<String, Plugin> installable = pluginManager.getInstallablePlugins();
                    if (installable.containsKey(m)) {
                        Plugin candidate = installable.get(m);
                        PluginListItem item = new PluginListItem(false, candidate);
                        PluginListTable table = new PluginListTable(List.of(item));
                        output.info("Plugin %s is available:\n%s", m,
                                table.getContent());
                        if (interactiveMode && Prompt.yesOrNo(true,
                                "Would you like to install it now?",
                                args)) {
                            pluginManager.addPlugin(m).ifPresent(added -> plugins.put(added.getName(), added));
                            pluginCommandFactory.populateCommands(cmd, plugins);
                        }
                    } else {
                        output.error("Unable to match command `%s` and a corresponding plugin couldn't be installed.", m);
                    }
                } catch (Exception e) {
                    output.error("Unable to match command `%s` and a corresponding plugin couldn't be installed.", m);
                }
            });
        } catch (MutuallyExclusiveArgsException e) {
            return ExitCode.USAGE;
        }
        return cmd.execute(args);
    }

    /**
     * Resolve the command path from arguments without parsing options.
     * Stops at the first flag ({@code -...}), {@code --}, or end of arguments.
     */
    static List<String> extractCommandPath(String[] args) {
        List<String> path = new ArrayList<>();
        boolean endOfOptions = false;
        for (String arg : args) {
            if (endOfOptions) {
                path.add(arg);
                continue;
            }
            if ("--".equals(arg)) {
                endOfOptions = true;
                continue;
            }
            if (arg.startsWith("-")) {
                break;
            }
            path.add(arg);
        }
        return path;
    }

    static CommandLine findSubcommand(CommandLine parent, String name) {
        Map<String, CommandLine> subcommands = parent.getSubcommands();
        CommandLine subcommand = subcommands.get(name);
        if (subcommand != null) {
            return subcommand;
        }
        for (CommandLine candidate : subcommands.values()) {
            if (candidate.getCommandSpec().names().contains(name)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Determine whether the command path refers to a missing subcommand by walking
     * the command tree using positional tokens only (no option parsing).
     * <p>
     * Once a leaf command (no subcommands) is reached, remaining tokens are treated as
     * parameters rather than missing subcommands. Catch-all commands (e.g. {@code create})
     * always resolve as present so hybrid usage like {@code create my-project} does not
     * trigger plugin lookup.
     *
     * @param root the root command
     * @param args the arguments passed to the root command
     * @return the missing subcommand wrapped in {@link Optional} or empty if no subcommand is missing
     */
    public Optional<String> findMissingCommand(CommandLine root, String[] args) {
        List<String> path = extractCommandPath(args);
        if (path.isEmpty()) {
            return Optional.empty();
        }

        CommandLine current = root;
        StringBuilder commandId = new StringBuilder();

        for (int i = 0; i < path.size(); i++) {
            String segment = path.get(i);
            CommandLine next = findSubcommand(current, segment);
            if (next == null) {
                // Leaf command: leftover positional tokens are parameters (e.g. encrypt/decrypt).
                if (current.getSubcommands().isEmpty()) {
                    return Optional.empty();
                }
                StringBuilder missing = new StringBuilder(commandId);
                if (!missing.isEmpty()) {
                    missing.append("-");
                }
                // Only the first unmatched segment is the candidate plugin/command name.
                missing.append(segment);
                return Optional.of(missing.toString());
            }

            current = next;
            String name = current.getCommandSpec().name();
            if (CATCH_ALL_COMMANDS.contains(name)) {
                return Optional.empty();
            }

            if (!commandId.isEmpty()) {
                commandId.append("-");
            }
            commandId.append(name);
        }

        return Optional.empty();
    }

    @Override
    public Integer call() throws Exception {
        output.info("%n@|bold Quarkus CLI|@ version %s", VersionHelper.clientVersion());
        output.info("");
        output.info("Create Quarkus projects with Maven, Gradle, or JBang.");
        output.info("Manage extensions and source registries.");
        output.info("");
        output.info("Create: @|bold quarkus create|@");
        output.info("@|italic Iterate|@: @|bold quarkus dev|@");
        output.info("Build and test: @|bold quarkus build|@");
        output.info("");
        output.info("Find more information at https://quarkus.io");
        output.info("If you have questions, check https://github.com/quarkusio/quarkus/discussions");

        spec.commandLine().usage(output.out());

        output.info("");
        output.info("Use \"quarkus <command> --help\" for more information about a given command.");

        return spec.exitCodeOnUsageHelp();
    }

    class ShortErrorMessageHandler implements IParameterExceptionHandler {
        public int handleParseException(ParameterException ex, String[] args) {
            CommandLine cmd = ex.getCommandLine();
            CommandSpec spec = cmd.getCommandSpec();

            output.error(ex.getMessage()); // bold red
            output.printStackTrace(ex);

            UnmatchedArgumentException.printSuggestions(ex, output.err());
            output.err().println(cmd.getHelp().fullSynopsis()); // normal text to error stream

            if (spec.equals(spec.root())) {
                output.err().println(cmd.getHelp().commandList()); // normal text to error stream
            }
            output.err().printf("See '%s --help' for more information.%n", spec.qualifiedName());
            output.err().flush();

            return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : spec.exitCodeOnInvalidInput();
        }
    }

    class SubCommandListRenderer implements IHelpSectionRenderer {
        // @Override
        public String render(Help help) {
            CommandSpec spec = help.commandSpec();
            if (spec.subcommands().isEmpty()) {
                return "";
            }

            Help.Column commands = new Help.Column(24, 2, CommandLine.Help.Column.Overflow.SPAN);
            Help.Column descriptions = new Help.Column(spec.usageMessage().width() - 24, 2,
                    CommandLine.Help.Column.Overflow.WRAP);
            Help.TextTable textTable = Help.TextTable.forColumns(help.colorScheme(), commands, descriptions);
            textTable.setAdjustLineBreaksForWideCJKCharacters(spec.usageMessage().adjustLineBreaksForWideCJKCharacters());

            addHierarchy(spec.subcommands().values(), textTable, "");
            return textTable.toString();
        }

        private void addHierarchy(Collection<CommandLine> collection, Help.TextTable textTable,
                String indent) {
            collection.stream().distinct().forEach(subcommand -> {
                // create comma-separated list of command name and aliases
                String names = String.join(", ", subcommand.getCommandSpec().names());
                String description = description(subcommand.getCommandSpec().usageMessage());
                textTable.addRowValues(indent + names, description);

                Map<String, CommandLine> subcommands = subcommand.getSubcommands();
                if (!subcommands.isEmpty()) {
                    addHierarchy(subcommands.values(), textTable, indent + "  ");
                }
            });
        }

        private String description(UsageMessageSpec usageMessage) {
            if (usageMessage.header().length > 0) {
                return usageMessage.header()[0];
            }
            if (usageMessage.description().length > 0) {
                return usageMessage.description()[0];
            }
            return "";
        }
    }

    private Optional<Path> getProjectRoot(Optional<String> testDir) {
        Path projectRoot = testDir.map(Paths::get).orElse(null);

        if (projectRoot == null) {
            projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        }
        return Optional.ofNullable(projectRoot);
    }

    private Supplier<QuarkusProject> quarkusProject(Optional<String> testDir) {
        Path root = getProjectRoot(testDir).orElseThrow();
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(root);
        if (buildTool == null) {
            return () -> null;
        }
        return () -> {
            try {
                return registryClient.createQuarkusProject(root, new TargetQuarkusPlatformGroup(), buildTool, output);
            } catch (Exception e) {
                return null;
            }
        };
    }

    private PluginManager pluginManager(OutputOptionMixin output, Optional<String> testDir, boolean interactiveMode) {
        PluginManagerSettings settings = PluginManagerSettings.defaultSettings()
                .withInteractivetMode(interactiveMode); // Why not just getting it from output.isClieTest ? Cause args have not been parsed yet.
        return PluginManager.create(settings, output, Optional.ofNullable(Paths.get(System.getProperty("user.home"))),
                getProjectRoot(testDir), quarkusProject(testDir));
    }
}
