package io.quarkus.cli;

import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.runtime.QuarkusApplication;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.IHelpSectionRenderer;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.UsageMessageSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.UnmatchedArgumentException;

@CommandLine.Command(name = "quarkus", subcommands = {
        Create.class, Build.class, Dev.class, ProjectExtensions.class, Registry.class, Info.class, Update.class, Version.class,
        Completion.class }, scope = ScopeType.INHERIT, sortOptions = false, showDefaultValues = true, versionProvider = Version.class, subcommandsRepeatable = false, mixinStandardHelpOptions = false, commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "Options:%n", headerHeading = "%n", parameterListHeading = "%n")
public class QuarkusCli implements QuarkusApplication, Callable<Integer> {
    static {
        System.setProperty("picocli.endofoptions.description", "End of command line options.");
    }

    @Inject
    CommandLine.IFactory factory;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Option(names = { "-v", "--version" }, versionHelp = true, description = "Print version information and exit.")
    public boolean showVersion;

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.ArgGroup(exclusive = false, validate = false)
    protected PropertiesOptions propertiesOptions = new PropertiesOptions();

    @Override
    public int run(String... args) throws Exception {
        CommandLine cmd = factory == null ? new CommandLine(this) : new CommandLine(this, factory);
        cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, new SubCommandListRenderer());
        cmd.setParameterExceptionHandler(new ShortErrorMessageHandler());
        return cmd.execute(args);
    }

    @Override
    public Integer call() throws Exception {
        output.info("%n@|bold Quarkus CLI|@ version %s", Version.clientVersion());
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

}
