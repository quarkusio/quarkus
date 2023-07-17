package io.quarkus.cli;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.cli.common.DataOptions;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.TargetQuarkusPlatformGroup;
import io.quarkus.cli.create.BaseCreateCommand;
import io.quarkus.cli.create.CodeGenerationGroup;
import io.quarkus.cli.create.TargetBuildToolGroup;
import io.quarkus.cli.create.TargetGAVGroup;
import io.quarkus.cli.create.TargetLanguageGroup;
import io.quarkus.devtools.commands.CreateProject.CreateProjectKey;
import io.quarkus.devtools.commands.SourceType;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.handlers.CreateJBangProjectCommandHandler;
import io.quarkus.devtools.commands.handlers.CreateProjectCommandHandler;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "app", header = "Create a Quarkus application project.", description = "%n"
        + "This command will create a Java project in a new ARTIFACT-ID directory", footer = { "%n"
                + "For example (using default values), a new Java project will be created in a 'code-with-quarkus' directory; "
                + "it will use Maven to build an artifact with GROUP-ID='org.acme', ARTIFACT-ID='code-with-quarkus', and VERSION='1.0.0-SNAPSHOT'."
                + "%n" })
public class CreateApp extends BaseCreateCommand {
    @CommandLine.Mixin
    TargetGAVGroup gav = new TargetGAVGroup();

    @CommandLine.Option(order = 1, paramLabel = "EXTENSION", names = { "-x",
            "--extension", "--extensions" }, description = "Extension(s) to add to the project.", split = ",")
    Set<String> extensions = new HashSet<>();

    @CommandLine.Option(order = 2, paramLabel = "NAME", names = { "--name" }, description = "Name of the project.")
    String name;

    @CommandLine.Option(order = 3, paramLabel = "DESCRIPTION", names = {
            "--description" }, description = "Description of the project.")
    String description;

    @CommandLine.ArgGroup(order = 4, heading = "%nQuarkus version:%n")
    TargetQuarkusPlatformGroup targetQuarkusVersion = new TargetQuarkusPlatformGroup();

    @CommandLine.ArgGroup(order = 5, heading = "%nBuild tool (Maven):%n")
    TargetBuildToolGroup targetBuildTool = new TargetBuildToolGroup();

    @CommandLine.ArgGroup(order = 6, exclusive = false, heading = "%nTarget language:%n")
    TargetLanguageGroup targetLanguage = new TargetLanguageGroup();

    @CommandLine.ArgGroup(order = 7, exclusive = false, heading = "%nCode Generation:%n")
    CodeGenerationGroup codeGeneration = new CodeGenerationGroup();

    @CommandLine.ArgGroup(order = 8, exclusive = false, validate = false)
    DataOptions dataOptions = new DataOptions();

    @CommandLine.ArgGroup(order = 9, exclusive = false, validate = false)
    PropertiesOptions propertiesOptions = new PropertiesOptions();

    @Override
    public Integer call() throws Exception {
        try {
            output.debug("Creating a new project with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            setSingleProjectGAV(gav);
            setTestOutputDirectory(output.getTestDirectory());
            if (checkProjectRootAlreadyExists(runMode.isDryRun())) {
                return CommandLine.ExitCode.USAGE;
            }

            BuildTool buildTool = targetBuildTool.getBuildTool(BuildTool.MAVEN);
            SourceType sourceType = targetLanguage.getSourceType(spec, buildTool, extensions, output);
            setJavaVersion(sourceType, targetLanguage.getJavaVersion());
            setSourceTypeExtensions(extensions, sourceType);
            setCodegenOptions(codeGeneration);
            setValue(CreateProjectKey.PROJECT_NAME, name);
            setValue(CreateProjectKey.PROJECT_DESCRIPTION, description);
            setValue(CreateProjectKey.DATA, dataOptions.data);

            QuarkusCommandInvocation invocation = build(buildTool, targetQuarkusVersion,
                    propertiesOptions.properties, extensions);

            boolean success = true;

            if (runMode.isDryRun()) {
                dryRun(buildTool, invocation, output);
            } else if (buildTool == BuildTool.JBANG) {
                success = new CreateJBangProjectCommandHandler().execute(invocation).isSuccess();
            } else { // maven or gradle
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
                    "Unable to create project: " + e.getLocalizedMessage());
        }
    }

    @Override
    public String toString() {
        return "CreateApp{"
                + "gav=" + gav
                + ", quarkusVersion=" + targetQuarkusVersion
                + ", targetBuildTool=" + targetBuildTool
                + ", targetLanguage=" + targetLanguage
                + ", codeGeneration=" + codeGeneration
                + ", extensions=" + extensions
                + ", name=" + name
                + ", description=" + description
                + ", project=" + super.toString()
                + ", data=" + dataOptions.data
                + ", properties=" + propertiesOptions.properties
                + '}';
    }
}
