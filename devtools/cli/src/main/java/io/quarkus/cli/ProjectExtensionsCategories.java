package io.quarkus.cli;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.CategoryListFormatOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.devtools.commands.ListCategories;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.registry.RegistryResolutionException;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

@CommandLine.Command(name = "categories", aliases = "cat", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "List extension categories.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "%nOptions:%n")
public class ProjectExtensionsCategories extends BaseBuildCommand implements Callable<Integer> {

    @Mixin
    RunModeOption runMode;

    @CommandLine.ArgGroup(heading = "%nOutput format:%n")
    CategoryListFormatOptions format = new CategoryListFormatOptions();

    @CommandLine.ArgGroup(order = 2, heading = "%nQuarkus version:%n")
    TargetQuarkusVersionGroup targetQuarkusVersion = new TargetQuarkusVersionGroup();

    @Override
    public Integer call() {
        try {
            output.debug("List extension categories with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            // Test for an existing project
            BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectRoot()); // nullable

            if (buildTool == null || targetQuarkusVersion.isPlatformSpecified() || targetQuarkusVersion.isStreamSpecified()) {
                if (runMode.isDryRun()) {
                    return dryRunList(spec.commandLine().getHelp(), null);
                }

                Integer exitCode = listPlatformCategories();
                printHints(!format.isSpecified(), true);
                return exitCode;
            } else {
                BuildSystemRunner runner = getRunner();
                if (runMode.isDryRun()) {
                    return dryRunList(spec.commandLine().getHelp(), runner.getBuildTool());
                }

                Integer exitCode = runner.listExtensionCategories(runMode, format);
                printHints(!format.isSpecified(), true);
                return exitCode;
            }
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to list extension categories: " + e.getMessage());
        }
    }

    Integer dryRunList(CommandLine.Help help, BuildTool buildTool) {
        Map<String, String> dryRunOutput = new TreeMap<>();

        if (buildTool == null) {
            output.printText(new String[] {
                    "\nList extension categories for specified platform\n",
                    "\t" + targetQuarkusVersion.dryRun()
            });
        } else {
            output.printText(new String[] {
                    "\nList extension categories for current project\n",
                    "\t" + projectRoot().toString()
            });
            dryRunOutput.put("Build tool", buildTool.name());
        }

        dryRunOutput.put("Batch (non-interactive mode)", Boolean.toString(runMode.isBatchMode()));
        dryRunOutput.put("List format", format.getFormatString());

        output.info(help.createTextTable(dryRunOutput).toString());
        return CommandLine.ExitCode.OK;
    }

    Integer listPlatformCategories() throws QuarkusCommandException, RegistryResolutionException {
        QuarkusProject qp = registryClient.createQuarkusProject(projectRoot(), targetQuarkusVersion,
                BuildTool.MAVEN, output);

        QuarkusCommandOutcome outcome = new ListCategories(qp, output)
                .fromCli(true)
                .format(format.getFormatString())
                .batchMode(runMode.isBatchMode())
                .execute();

        return outcome.isSuccess() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
    }

    private void printHints(boolean formatHint, boolean extensionListHint) {
        if (runMode.isBatchMode())
            return;

        if (formatHint) {
            output.info("");
            output.info(ListCategories.MORE_INFO_HINT, "--full");
        }

        if (extensionListHint) {
            output.info("");
            output.info(ListCategories.LIST_EXTENSIONS_HINT,
                    "`quarkus extension list --installable --category \"categoryId\"`");
        }
    }

    @Override
    public String toString() {
        return "ProjectExtensionsCategories [output=" + output
                + ", runMode=" + runMode
                + "]";
    }
}
