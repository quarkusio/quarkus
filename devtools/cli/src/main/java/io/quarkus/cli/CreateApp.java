package io.quarkus.cli;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.cli.create.BaseCreateCommand;
import io.quarkus.cli.create.CodeGenerationGroup;
import io.quarkus.cli.create.CreateProjectMixin;
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

@CommandLine.Command(name = "app", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Create a Quarkus application project.", description = "%n"
        + "This command will create a Java project in a new ARTIFACT-ID directory. ", footer = { "%n"
                + "Using default values: a new Java project will be created in a 'code-with-quarkus' directory; "
                + "it will use Maven to build an artifact with groupId='org.acme', artifactId='code-with-quarkus', and version='1.0.0-SNAPSHOT'.%n" })
public class CreateApp extends BaseCreateCommand {
    @Mixin
    CreateProjectMixin createProject;

    @CommandLine.ArgGroup(order = 1, exclusive = false, heading = "%nProject identifiers%n")
    TargetGAVGroup gav = new TargetGAVGroup();

    @CommandLine.ArgGroup(order = 2, heading = "%nQuarkus version%n")
    TargetQuarkusVersionGroup targetQuarkusVersion = new TargetQuarkusVersionGroup();

    @CommandLine.ArgGroup(order = 3, heading = "%nBuild tool (Default: Maven)%n")
    TargetBuildToolGroup targetBuildTool = new TargetBuildToolGroup();

    @CommandLine.ArgGroup(order = 4, heading = "%nTarget language (Default: Java)%n")
    TargetLanguageGroup targetLanguage = new TargetLanguageGroup();

    @CommandLine.ArgGroup(order = 5, exclusive = false, heading = "%nCode Generation%n")
    CodeGenerationGroup codeGeneration = new CodeGenerationGroup();

    @CommandLine.ArgGroup(order = 6, exclusive = false, validate = false)
    PropertiesOptions propertiesOptions = new PropertiesOptions();

    @CommandLine.Parameters(arity = "0..1", paramLabel = "EXTENSION", description = "Extension(s) to add to the project.")
    Set<String> extensions = new HashSet<>();

    @Override
    public Integer call() throws Exception {
        try {
            output.debug("Creating a new project with initial parameters: %s", this);
            output.throwIfUnmatchedArguments(spec.commandLine());

            createProject.setSingleProjectGAV(gav);
            createProject.setTestOutputDirectory(output.getTestDirectory());
            createProject.projectRoot(); // verify project directories early

            BuildTool buildTool = targetBuildTool.getBuildTool(BuildTool.MAVEN);
            SourceType sourceType = targetLanguage.getSourceType(buildTool, extensions, output);
            createProject.setSourceTypeExtensions(extensions, sourceType);
            createProject.setCodegenOptions(codeGeneration);

            QuarkusCommandInvocation invocation = createProject.build(buildTool, targetQuarkusVersion,
                    output, propertiesOptions.properties);

            boolean success = true;

            if (runMode.isDryRun()) {
                createProject.dryRun(buildTool, invocation, output);
            } else if (buildTool == null || buildTool == BuildTool.JBANG) { // buildless / JBang
                success = new CreateJBangProjectCommandHandler().execute(invocation).isSuccess();
            } else { // maven or gradle
                success = new CreateProjectCommandHandler().execute(invocation).isSuccess();
            }
            return success ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
        } catch (Exception e) {
            return output.handleCommandException(e,
                    "Unable to create project: " + e.getMessage());
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
                + ", project=" + createProject
                + ", properties=" + propertiesOptions.properties
                + '}';
    }
}
