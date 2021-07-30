package io.quarkus.cli;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.cli.common.ListFormatOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.registry.RegistryResolutionException;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

@CommandLine.Command(name = "list", aliases = "ls", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "List platforms and extensions. ", footer = {
        "%nList modes:%n",
        "(relative). Active when invoked within a project unless an explicit release is specified. " +
                "The current project configuration will determine what extensions are listed, " +
                "with installed (available) extensions listed by default.%n",
        "(absolute). Active when invoked outside of a project or when an explicit release is specified. " +
                "All extensions for the specified release will be listed. " +
                "The CLI release will be used if this command is invoked outside of a project and no other release is specified.%n" }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "%nOptions:%n")
public class ProjectExtensionsList extends BaseBuildCommand implements Callable<Integer> {

    @Mixin
    RunModeOption runMode;

    @CommandLine.ArgGroup(order = 2, heading = "%nQuarkus version (absolute):%n")
    TargetQuarkusVersionGroup targetQuarkusVersion = new TargetQuarkusVersionGroup();

    @CommandLine.Option(names = { "-i",
            "--installable" }, defaultValue = "false", order = 2, description = "List extensions that can be installed (relative)")
    boolean installable = false;

    @CommandLine.Option(names = { "-s",
            "--search" }, defaultValue = "*", paramLabel = "PATTERN", order = 3, description = "Search for matching extensions (simple glob using '*' and '?').")
    String searchPattern;

    @CommandLine.Option(names = { "-c",
            "--category" }, defaultValue = "", paramLabel = "CATEGORY_ID", order = 4, description = "Only list extensions from the specified category.")
    String category;

    @CommandLine.ArgGroup(heading = "%nOutput format:%n")
    ListFormatOptions format = new ListFormatOptions();

    @Override
    public Integer call() {
        try {
            output.debug("List extensions with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            // Test for an existing project
            BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectRoot()); // nullable
            boolean categorySet = category != null && !category.isBlank();

            if (buildTool == null || targetQuarkusVersion.isPlatformSpecified() || targetQuarkusVersion.isStreamSpecified()) {
                // do not evaluate installables for list of arbitrary version (project-agnostic)
                installable = false;
                // check if any format was specified
                boolean formatSpecified = format.isSpecified();
                // show origins by default
                format.useOriginsUnlessSpecified();

                if (runMode.isDryRun()) {
                    return dryRunList(spec.commandLine().getHelp(), null);
                }

                Integer exitCode = listPlatformExtensions();
                printHints(buildTool, !formatSpecified, !categorySet, buildTool != null);
                return exitCode;
            } else {
                BuildSystemRunner runner = getRunner();

                if (runMode.isDryRun()) {
                    return dryRunList(spec.commandLine().getHelp(), runner.getBuildTool());
                }

                Integer exitCode = runner.listExtensions(runMode, format, installable, searchPattern, category);
                printHints(buildTool, !format.isSpecified(), installable && !categorySet, installable);
                return exitCode;
            }
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to list extensions: " + e.getMessage());
        }
    }

    Integer dryRunList(CommandLine.Help help, BuildTool buildTool) {
        Map<String, String> dryRunOutput = new TreeMap<>();

        if (buildTool == null) {
            output.printText(new String[] {
                    "\nList extensions for specified platform\n",
                    "\t" + targetQuarkusVersion.dryRun()
            });
        } else {
            output.printText(new String[] {
                    "\nList extensions for current project\n",
                    "\t" + projectRoot().toString()
            });
            dryRunOutput.put("Build tool", buildTool.name());
        }

        dryRunOutput.put("Batch (non-interactive mode)", Boolean.toString(runMode.isBatchMode()));
        dryRunOutput.put("List format", format.getFormatString());
        dryRunOutput.put("List installable extensions", Boolean.toString(installable));
        dryRunOutput.put("Search pattern", searchPattern);
        dryRunOutput.put("Category", category);
        dryRunOutput.put("Registry Client", Boolean.toString(registryClient.enabled()));

        output.info(help.createTextTable(dryRunOutput).toString());
        return CommandLine.ExitCode.OK;
    }

    Integer listPlatformExtensions() throws QuarkusCommandException, RegistryResolutionException {
        QuarkusProject qp = registryClient.createQuarkusProject(projectRoot(), targetQuarkusVersion,
                BuildTool.MAVEN, output);

        QuarkusCommandOutcome outcome = new ListExtensions(qp, output)
                .fromCli(true)
                .all(true)
                .format(format.getFormatString())
                .search(searchPattern)
                .category(category)
                .batchMode(runMode.isBatchMode())
                .execute();

        return outcome.isSuccess() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
    }

    private void printHints(BuildTool buildTool, boolean formatHint, boolean filterHint, boolean addExtensionHint) {
        if (runMode.isBatchMode())
            return;

        if (formatHint) {
            output.info("");
            output.info(ListExtensions.MORE_INFO_HINT, "--full");
        }

        if (filterHint) {
            output.info("");
            output.info(ListExtensions.FILTER_HINT, "--category \"categoryId\"");
        }

        if (addExtensionHint) {
            output.info("");
            if (BuildTool.GRADLE.equals(buildTool) || BuildTool.GRADLE_KOTLIN_DSL.equals(buildTool)) {
                output.info(ListExtensions.ADD_EXTENSION_HINT, "build.gradle", "quarkus extension add \"artifactId\"");
            } else if (BuildTool.MAVEN.equals(buildTool)) {
                output.info(ListExtensions.ADD_EXTENSION_HINT, "pom.xml", "quarkus extension add \"artifactId\"");
            }
        }
    }

    @Override
    public String toString() {
        return "ProjectExtensionList [format=" + format
                + ", installable=" + installable
                + ", searchPattern=" + searchPattern
                + ", output=" + output
                + ", runMode=" + runMode
                + "]";
    }
}
