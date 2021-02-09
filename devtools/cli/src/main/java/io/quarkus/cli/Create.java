package io.quarkus.cli;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "create", sortOptions = false, usageHelpAutoWidth = true, mixinStandardHelpOptions = false, description = "Create a new quarkus project.")
public class Create extends BaseSubCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-g",
            "--group-id" }, order = 1, paramLabel = "GROUP-ID", description = "The groupId for project")
    String groupId = "org.acme";

    @CommandLine.Option(names = { "-a",
            "--artifact-id" }, order = 2, paramLabel = "ARTIFACT-ID", description = "The artifactId for project")
    String artifactId = "code-with-quarkus";

    @CommandLine.Option(names = { "-v",
            "--version" }, order = 3, paramLabel = "VERSION", description = "The version for project")
    String version = "1.0.0-SNAPSHOT";

    @CommandLine.Option(names = { "-0",
            "--no-examples" }, order = 4, description = "Generate without example code.")
    boolean noExamples = false;

    @CommandLine.Option(names = { "-e",
            "--examples" }, order = 4, description = "Choose which example(s) you want in the generated Quarkus application.")
    Set<String> examples;

    @CommandLine.ArgGroup()
    TargetBuildTool targetBuildTool = new TargetBuildTool();

    static class TargetBuildTool {
        @CommandLine.Option(names = { "--maven" }, order = 5, description = "Create a Maven project. (default)")
        boolean maven = false;

        @CommandLine.Option(names = { "--gradle" }, order = 6, description = "Create a Gradle project.")
        boolean gradle = false;

        @CommandLine.Option(names = {
                "--grade-kotlin-dsl" }, order = 7, description = "Create a Gradle Kotlin DSL project.")
        boolean gradleKotlinDsl = false;
    }

    @CommandLine.ArgGroup()
    TargetLanguage language = new TargetLanguage();

    static class TargetLanguage {
        @CommandLine.Option(names = {
                "--java" }, order = 8, description = "Generate Java examples. (default)")
        boolean java = false;

        @CommandLine.Option(names = {
                "--kotlin" }, order = 9, description = "Generate Kotlin examples.")
        boolean kotlin = false;

        @CommandLine.Option(names = {
                "--scala" }, order = 10, description = "Generate Scala examples.")
        boolean scala = false;
    }

    @CommandLine.Parameters(arity = "0..1", paramLabel = "EXTENSION", description = "extension to add to project")
    Set<String> extensions;

    @Override
    public Integer call() throws Exception {
        try {
            File projectDirectory = new File(System.getProperty("user.dir"));

            File projectRoot = new File(projectDirectory.getAbsoluteFile(), artifactId);
            if (projectRoot.exists()) {
                err().println("Unable to create the project, " +
                        "the directory " + projectRoot.getAbsolutePath() + " already exists");
                return CommandLine.ExitCode.SOFTWARE;
            }

            SourceType sourceType = SourceType.JAVA;
            if (targetBuildTool.gradleKotlinDsl) {
                sourceType = SourceType.KOTLIN;
                if (extensions == null)
                    extensions = new HashSet<>();
                if (!extensions.contains("kotlin"))
                    extensions.add("quarkus-kotlin");
            } else if (language.scala) {
                sourceType = SourceType.SCALA;
                if (extensions == null)
                    extensions = new HashSet<>();
                if (!extensions.contains("scala"))
                    extensions.add("quarkus-scala");
            } else if (language.kotlin) {
                sourceType = SourceType.KOTLIN;
                if (extensions == null)
                    extensions = new HashSet<>();
                if (!extensions.contains("kotlin"))
                    extensions.add("quarkus-kotlin");
            } else if (extensions != null && !extensions.isEmpty()) {
                sourceType = CreateProject.determineSourceType(extensions);
            }

            BuildTool buildTool = BuildTool.MAVEN;
            if (targetBuildTool.gradle)
                buildTool = BuildTool.GRADLE;
            else if (targetBuildTool.gradleKotlinDsl)
                buildTool = BuildTool.GRADLE_KOTLIN_DSL;

            boolean status = new CreateProject(projectRoot.getAbsoluteFile().toPath(),
                    QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor())
                            .buildTool(buildTool)
                            .groupId(groupId)
                            .artifactId(artifactId)
                            .version(version)
                            .sourceType(sourceType)
                            .overrideExamples(examples)
                            .extensions(extensions)
                            .noExamples(noExamples)
                            .execute().isSuccess();

            if (status) {
                out().println("Project " + artifactId +
                        " created.");
                parent.setProjectDirectory(projectRoot.toPath().toAbsolutePath());
            } else {
                err().println("Failed to create project");
                return CommandLine.ExitCode.SOFTWARE;
            }
        } catch (Exception e) {
            if (parent.showErrors)
                e.printStackTrace(err());
            err().println("Project creation failed, " + e.getMessage());
            return CommandLine.ExitCode.SOFTWARE;
        }

        return CommandLine.ExitCode.OK;
    }
}
