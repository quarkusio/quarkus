package io.quarkus.cli;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.cli.create.BaseCreateCommand;
import io.quarkus.cli.create.CodeGenerationGroup;
import io.quarkus.cli.create.TargetBuildToolGroup;
import io.quarkus.cli.create.TargetGAVGroup;
import io.quarkus.cli.create.TargetLanguageGroup;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.handlers.CreateJBangProjectCommandHandler;
import io.quarkus.devtools.commands.handlers.CreateProjectCommandHandler;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.codegen.SourceType;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

@CommandLine.Command(name = "cli", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Create a Quarkus command-line project.", description = "%n"
        + "This command will create a Java project in a new ARTIFACT-ID directory.", footer = { "%n"
                + "For example (using default values), a new Java project will be created in a 'code-with-quarkus' directory; "
                + "it will use Maven to build an artifact with GROUP-ID='org.acme', ARTIFACT-ID='code-with-quarkus', and VERSION='1.0.0-SNAPSHOT'."
                + "%n" }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class CreateCli extends BaseCreateCommand {
    @Mixin
    TargetGAVGroup gav = new TargetGAVGroup();

    @CommandLine.Option(order = 1, paramLabel = "EXTENSION", names = { "-x",
            "--extension" }, description = "Extension(s) to add to the project.", split = ",")
    Set<String> extensions = new HashSet<>();

    @CommandLine.ArgGroup(order = 2, heading = "%nQuarkus version:%n")
    TargetQuarkusVersionGroup targetQuarkusVersion = new TargetQuarkusVersionGroup();

    @CommandLine.ArgGroup(order = 3, heading = "%nBuild tool (Maven):%n")
    TargetBuildToolGroup targetBuildTool = new TargetBuildToolGroup();

    @CommandLine.ArgGroup(order = 4, heading = "%nTarget language (Java):%n")
    TargetLanguageGroup targetLanguage = new TargetLanguageGroup();

    @CommandLine.ArgGroup(order = 5, exclusive = false, heading = "%nCode Generation:%n")
    CodeGenerationGroup codeGeneration = new CodeGenerationGroup();

    @CommandLine.ArgGroup(order = 6, exclusive = false, validate = false)
    PropertiesOptions propertiesOptions = new PropertiesOptions();

    @Override
    public Integer call() throws Exception {
        try {
            output.debug("Creating a new project with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            extensions.add("picocli"); // make sure picocli is selected.

            setSingleProjectGAV(gav);
            setTestOutputDirectory(output.getTestDirectory());
            if (checkProjectRootAlreadyExists(false)) {
                return CommandLine.ExitCode.USAGE;
            }

            BuildTool buildTool = targetBuildTool.getBuildTool(BuildTool.MAVEN);
            SourceType sourceType = targetLanguage.getSourceType(buildTool, extensions, output);
            setSourceTypeExtensions(extensions, sourceType);
            setCodegenOptions(codeGeneration);

            QuarkusCommandInvocation invocation = build(buildTool, targetQuarkusVersion,
                    propertiesOptions.properties);

            boolean success = true;
            if (runMode.isDryRun()) {
                dryRun(buildTool, invocation, output);
            } else if (BuildTool.JBANG.equals(buildTool)) {
                success = new CreateJBangProjectCommandHandler().execute(invocation).isSuccess();
            } else {
                success = new CreateProjectCommandHandler().execute(invocation).isSuccess();
            }

            if (success) {
                if (!runMode.isDryRun()) {
                    output.info(
                            "Navigate into this directory and get started: " + spec.root().qualifiedName() + " dev");
                }
                return CommandLine.ExitCode.OK;
            }
            return CommandLine.ExitCode.SOFTWARE;
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to create project: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "CreateCli{"
                + "gav=" + gav
                + ", quarkusVersion=" + targetQuarkusVersion
                + ", targetBuildTool=" + targetBuildTool
                + ", targetLanguage=" + targetLanguage
                + ", codeGeneration=" + codeGeneration
                + ", extensions=" + extensions
                + ", project=" + super.toString()
                + ", properties=" + propertiesOptions.properties
                + '}';
    }
}
