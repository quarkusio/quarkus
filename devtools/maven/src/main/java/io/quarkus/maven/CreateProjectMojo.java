package io.quarkus.maven;

import static org.fusesource.jansi.Ansi.ansi;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.fusesource.jansi.Ansi;

import io.quarkus.cli.commands.AddExtensionResult;
import io.quarkus.cli.commands.AddExtensions;
import io.quarkus.cli.commands.CreateProject;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.SourceType;
import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.components.Prompter;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * This goal helps in setting up Quarkus Maven project with quarkus-maven-plugin, with sensible defaults
 */
@Mojo(name = "create", requiresProject = false)
public class CreateProjectMojo extends AbstractMojo {

    public static final String PLUGIN_KEY = MojoUtils.getPluginGroupId() + ":" + MojoUtils.getPluginArtifactId();

    private static final String DEFAULT_GROUP_ID = "org.acme.quarkus.sample";

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "projectGroupId")
    private String projectGroupId;

    @Parameter(property = "projectArtifactId")
    private String projectArtifactId;

    @Parameter(property = "projectVersion")
    private String projectVersion;

    @Parameter(property = "path")
    private String path;

    @Parameter(property = "className")
    private String className;

    @Parameter(property = "buildTool", defaultValue = "MAVEN")
    private String buildTool;

    @Parameter(property = "extensions")
    private Set<String> extensions;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Component
    private Prompter prompter;

    @Component
    private MavenVersionEnforcer mavenVersionEnforcer;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private ProjectBuilder projectBuilder;

    @Override
    public void execute() throws MojoExecutionException {
        // We detect the Maven version during the project generation to indicate the user immediately that the installed
        // version may not be supported.
        mavenVersionEnforcer.ensureMavenVersion(getLog(), session);

        File projectRoot = new File(".");
        File pom = new File(projectRoot, "pom.xml");

        if (pom.isFile()) {
            // Enforce that the GAV are not set
            if (!StringUtils.isBlank(projectGroupId) || !StringUtils.isBlank(projectArtifactId)
                    || !StringUtils.isBlank(projectVersion)) {
                throw new MojoExecutionException("Unable to generate the project, the `projectGroupId`, " +
                        "`projectArtifactId` and `projectVersion` parameters are not supported when applied to an " +
                        "existing `pom.xml` file");
            }

            // Load the GAV from the existing project
            projectGroupId = project.getGroupId();
            projectArtifactId = project.getArtifactId();
            projectVersion = project.getVersion();

        } else {
            askTheUserForMissingValues();
            if (!isDirectoryEmpty(projectRoot)) {
                projectRoot = new File(projectArtifactId);
                if (projectRoot.exists()) {
                    throw new MojoExecutionException("Unable to create the project - the current directory is not empty and" +
                            " the directory " + projectArtifactId + " exists");
                }
            }
        }

        boolean success;
        try {
            sanitizeExtensions();
            final SourceType sourceType = CreateProject.determineSourceType(extensions);
            sanitizeOptions(sourceType);

            final Map<String, Object> context = new HashMap<>();
            context.put("path", path);

            BuildTool buildToolEnum;
            try {
                buildToolEnum = BuildTool.valueOf(buildTool.toUpperCase());
            } catch (IllegalArgumentException e) {
                String validBuildTools = String.join(",",
                        Arrays.asList(BuildTool.values()).stream().map(BuildTool::toString).collect(Collectors.toList()));
                throw new IllegalArgumentException("Choose a valid build tool. Accepted values are: " + validBuildTools);
            }

            success = new CreateProject(new FileProjectWriter(projectRoot))
                    .groupId(projectGroupId)
                    .artifactId(projectArtifactId)
                    .version(projectVersion)
                    .sourceType(sourceType)
                    .className(className)
                    .buildTool(buildToolEnum)
                    .doCreateProject(context);

            File createdDependenciesBuildFile = new File(projectRoot, buildToolEnum.getDependenciesFile());
            File buildFile = new File(createdDependenciesBuildFile.getAbsolutePath());
            if (success) {
                AddExtensionResult result = new AddExtensions(new FileProjectWriter(buildFile.getParentFile()), buildToolEnum)
                        .addExtensions(extensions);
                if (!result.succeeded()) {
                    success = false;
                }
            }
            if (BuildTool.MAVEN.equals(buildToolEnum)) {
                createMavenWrapper(createdDependenciesBuildFile);
            } else if (BuildTool.GRADLE.equals(buildToolEnum)) {
                createGradleWrapper(buildFile.getParentFile());
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        if (success) {
            printUserInstructions(projectRoot);
        } else {
            throw new MojoExecutionException("the project was created but it was unable to add the extensions");
        }
    }

    private void createGradleWrapper(File projectDirectory) {
        try {
            Runtime.getRuntime().exec("gradle wrapper", new String[0], projectDirectory);
        } catch (IOException e) {
            // no reason to fail if the wrapper could not be created
            getLog().error(
                    "Unable to install the Gradle wrapper (./gradlew) in the project. You need to have gradle installed to generate the wrapper files.",
                    e);
        }

    }

    private void createMavenWrapper(File createdPomFile) {
        try {
            // we need to modify the maven environment used by the wrapper plugin since the project could have been
            // created in a directory other than the current
            MavenProject newProject = projectBuilder.build(
                    createdPomFile, new DefaultProjectBuildingRequest(session.getProjectBuildingRequest())).getProject();

            MavenExecutionRequest newExecutionRequest = DefaultMavenExecutionRequest.copy(session.getRequest());
            newExecutionRequest.setBaseDirectory(createdPomFile.getParentFile());

            MavenSession newSession = new MavenSession(session.getContainer(), session.getRepositorySession(),
                    newExecutionRequest, session.getResult());
            newSession.setCurrentProject(newProject);

            executeMojo(
                    plugin(
                            groupId("io.takari"),
                            artifactId("maven"),
                            version(MojoUtils.getMavenWrapperVersion())),
                    goal("wrapper"),
                    configuration(
                            element(name("maven"), MojoUtils.getProposedMavenVersion())),
                    executionEnvironment(
                            newProject,
                            newSession,
                            pluginManager));
        } catch (Exception e) {
            // no reason to fail if the wrapper could not be created
            getLog().error("Unable to install the Maven wrapper (./mvnw) in the project", e);
        }
    }

    private void askTheUserForMissingValues() throws MojoExecutionException {

        // If the user has disabled the interactive mode or if the user has specified the artifactId, disable the
        // user interactions.
        if (!session.getRequest().isInteractiveMode() || shouldUseDefaults()) {
            // Inject default values in all non-set parameters
            if (StringUtils.isBlank(projectGroupId)) {
                projectGroupId = DEFAULT_GROUP_ID;
            }
            if (StringUtils.isBlank(projectArtifactId)) {
                projectArtifactId = "my-quarkus-project";
            }
            if (StringUtils.isBlank(projectVersion)) {
                projectVersion = "1.0-SNAPSHOT";
            }
            return;
        }

        try {
            if (StringUtils.isBlank(projectGroupId)) {
                projectGroupId = prompter.promptWithDefaultValue("Set the project groupId",
                        DEFAULT_GROUP_ID);
            }

            if (StringUtils.isBlank(projectArtifactId)) {
                projectArtifactId = prompter.promptWithDefaultValue("Set the project artifactId",
                        "my-quarkus-project");
            }

            if (StringUtils.isBlank(projectVersion)) {
                projectVersion = prompter.promptWithDefaultValue("Set the project version",
                        "1.0-SNAPSHOT");
            }

            if (StringUtils.isBlank(className)) {
                // Ask the user if he want to create a resource
                String answer = prompter.promptWithDefaultValue("Do you want to create a REST resource? (y/n)", "no");
                if (isTrueOrYes(answer)) {
                    String defaultResourceName = projectGroupId.replace("-", ".")
                            .replace("_", ".") + ".HelloResource";
                    className = prompter.promptWithDefaultValue("Set the resource classname", defaultResourceName);
                    if (StringUtils.isBlank(path)) {
                        path = prompter.promptWithDefaultValue("Set the resource path ", CreateUtils.getDerivedPath(className));
                    }
                } else {
                    className = null;
                    path = null;
                }
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Unable to get user input", e);
        }
    }

    private boolean shouldUseDefaults() {
        // Must be called before user input
        return projectArtifactId != null;

    }

    private boolean isTrueOrYes(String answer) {
        if (answer == null) {
            return false;
        }
        String content = answer.trim().toLowerCase();
        return "true".equalsIgnoreCase(content) || "yes".equalsIgnoreCase(content) || "y".equalsIgnoreCase(content);
    }

    private void sanitizeOptions(SourceType sourceType) {
        // If className is null, we won't create the REST resource,
        if (className != null) {
            className = sourceType.stripExtensionFrom(className);

            if (!className.contains(".")) {
                // No package name, inject one
                className = projectGroupId.replace("-", ".").replace("_", ".") + "." + className;
            }

            if (StringUtils.isBlank(path)) {
                path = "/hello";
            }

            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }
    }

    private void sanitizeExtensions() {
        extensions = extensions.stream().filter(Objects::nonNull).map(String::trim).collect(Collectors.toSet());
    }

    private void printUserInstructions(File root) {
        getLog().info("");
        getLog().info("========================================================================================");
        getLog().info(
                ansi().a("Your new application has been created in ").bold().a(root.getAbsolutePath()).boldOff().toString());
        getLog().info(ansi().a("Navigate into this directory and launch your application with ")
                .bold()
                .fg(Ansi.Color.CYAN)
                .a("mvn compile quarkus:dev")
                .reset()
                .toString());
        getLog().info(
                ansi().a("Your application will be accessible on ").bold().fg(Ansi.Color.CYAN).a("http://localhost:8080")
                        .reset().toString());
        getLog().info("========================================================================================");
        getLog().info("");
    }

    private boolean isDirectoryEmpty(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("The specified file must be a directory: " + dir.getAbsolutePath());
        }

        String[] children = dir.list();
        if (children == null) {
            // IO Issue
            throw new IllegalArgumentException("The specified directory cannot be accessed: " + dir.getAbsolutePath());
        }

        return children.length == 0;
    }
}
