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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.settings.Proxy;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.fusesource.jansi.Ansi;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.components.Prompter;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsUtils;

/**
 * This goal helps in setting up Quarkus Maven project with quarkus-maven-plugin, with sensible defaults
 */
@Mojo(name = "create", requiresProject = false)
public class CreateProjectMojo extends AbstractMojo {

    private static final String DEFAULT_GROUP_ID = "org.acme";
    private static final String DEFAULT_ARTIFACT_ID = "code-with-quarkus";
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_EXTENSIONS = "resteasy";

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "projectGroupId")
    private String projectGroupId;

    @Parameter(property = "projectArtifactId")
    private String projectArtifactId;

    @Parameter(property = "projectVersion")
    private String projectVersion;

    @Parameter(property = "legacyCodegen", defaultValue = "false")
    private boolean legacyCodegen;

    @Parameter(property = "noExamples", defaultValue = "false")
    private boolean noExamples;

    /**
     * Group ID of the target platform BOM
     */
    @Parameter(property = "platformGroupId", required = false)
    private String bomGroupId;

    /**
     * Artifact ID of the target platform BOM
     */
    @Parameter(property = "platformArtifactId", required = false)
    private String bomArtifactId;

    /**
     * Version of the target platform BOM
     */
    @Parameter(property = "platformVersion", required = false)
    private String bomVersion;

    @Parameter(property = "path")
    private String path;

    /**
     * This parameter is only working with the RESTEasy and Spring Web extensions and is going to be removed.
     * Use packageName instead.
     *
     * {@code className}
     */
    @Parameter(property = "className")
    @Deprecated
    private String className;

    /**
     * If not set, groupId will be used
     */
    @Parameter(property = "packageName")
    private String packageName;

    @Parameter(property = "buildTool", defaultValue = "MAVEN")
    private String buildTool;

    @Parameter(property = "extensions")
    private Set<String> extensions;

    @Parameter(property = "outputDirectory", defaultValue = "${basedir}")
    private File outputDirectory;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Component
    private Prompter prompter;

    @Component
    private MavenVersionEnforcer mavenVersionEnforcer;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private ProjectBuilder projectBuilder;

    @Component
    private RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepoManager;

    @Override
    public void execute() throws MojoExecutionException {

        final MavenArtifactResolver mvn;
        try {
            mvn = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .setRemoteRepositoryManager(remoteRepoManager)
                    .build();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
        }
        final QuarkusPlatformDescriptor platform = CreateUtils.resolvePlatformDescriptor(bomGroupId, bomArtifactId, bomVersion,
                mvn, getLog());

        // We detect the Maven version during the project generation to indicate the user immediately that the installed
        // version may not be supported.
        mavenVersionEnforcer.ensureMavenVersion(getLog(), session);
        try {
            Files.createDirectories(outputDirectory.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create directory " + outputDirectory, e);
        }

        File projectRoot = outputDirectory;
        File pom = project != null ? project.getFile() : null;
        Model parentPomModel = null;

        boolean containsAtLeastOneGradleFile = false;
        for (String gradleFile : Arrays.asList("build.gradle", "settings.gradle", "build.gradle.kts", "settings.gradle.kts")) {
            containsAtLeastOneGradleFile |= new File(projectRoot, gradleFile).isFile();
        }

        BuildTool buildToolEnum = BuildTool.findTool(buildTool);
        if (buildToolEnum == null) {
            String validBuildTools = String.join(",",
                    Arrays.asList(BuildTool.values()).stream().map(BuildTool::toString).collect(Collectors.toList()));
            throw new IllegalArgumentException("Choose a valid build tool. Accepted values are: " + validBuildTools);
        }
        if (BuildTool.MAVEN.equals(buildToolEnum)) {
            if (pom != null && pom.isFile()) {
                try {
                    parentPomModel = MojoUtils.readPom(pom);
                    if (!"pom".equals(parentPomModel.getPackaging())) {
                        throw new MojoExecutionException(
                                "The parent project must have a packaging type of POM. Current packaging: "
                                        + parentPomModel.getPackaging());
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Could not access parent pom.", e);
                }
            } else if (containsAtLeastOneGradleFile) {
                throw new MojoExecutionException(
                        "You are trying to create maven project in a directory that contains only gradle build files.");
            }
        } else if (BuildTool.GRADLE.equals(buildToolEnum) || BuildTool.GRADLE_KOTLIN_DSL.equals(buildToolEnum)) {
            if (containsAtLeastOneGradleFile) {
                throw new MojoExecutionException("Adding subprojects to gradle projects is not implemented.");
            } else if (pom != null && pom.isFile()) {
                throw new MojoExecutionException(
                        "You are trying to create gradle project in a directory that contains only maven build files.");
            }
        }

        askTheUserForMissingValues();
        projectRoot = new File(outputDirectory, projectArtifactId);
        if (projectRoot.exists()) {
            throw new MojoExecutionException("Unable to create the project, " +
                    "the directory " + projectRoot.getAbsolutePath() + " already exists");
        }

        boolean success;
        final Path projectDirPath = projectRoot.toPath();
        try {
            sanitizeExtensions();
            final SourceType sourceType = CreateProject.determineSourceType(extensions);
            sanitizeOptions(sourceType);

            final CreateProject createProject = new CreateProject(projectDirPath, platform)
                    .buildTool(buildToolEnum)
                    .groupId(projectGroupId)
                    .artifactId(projectArtifactId)
                    .version(projectVersion)
                    .sourceType(sourceType)
                    .className(className)
                    .packageName(packageName)
                    .extensions(extensions)
                    .legacyCodegen(legacyCodegen)
                    .noExamples(noExamples);
            if (path != null) {
                createProject.setValue("path", path);
            }

            success = createProject.execute().isSuccess();
            if (success && parentPomModel != null && BuildTool.MAVEN.equals(buildToolEnum)) {
                // Write to parent pom and submodule pom if project creation is successful
                if (!parentPomModel.getModules().contains(this.projectArtifactId)) {
                    parentPomModel.addModule(this.projectArtifactId);
                }
                File subModulePomFile = new File(projectRoot, buildToolEnum.getDependenciesFile());
                Model subModulePomModel = MojoUtils.readPom(subModulePomFile);
                Parent parent = new Parent();
                parent.setGroupId(parentPomModel.getGroupId());
                parent.setArtifactId(parentPomModel.getArtifactId());
                parent.setVersion(parentPomModel.getVersion());
                subModulePomModel.setParent(parent);
                MojoUtils.write(parentPomModel, pom);
                MojoUtils.write(subModulePomModel, subModulePomFile);
            }
            if (legacyCodegen) {
                File createdDependenciesBuildFile = new File(projectRoot, buildToolEnum.getDependenciesFile());
                if (BuildTool.MAVEN.equals(buildToolEnum)) {
                    createMavenWrapper(createdDependenciesBuildFile, ToolsUtils.readQuarkusProperties(platform));
                } else if (BuildTool.GRADLE.equals(buildToolEnum) || BuildTool.GRADLE_KOTLIN_DSL.equals(buildToolEnum)) {
                    createGradleWrapper(platform, projectDirPath);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate Quarkus project", e);
        }
        if (success) {
            printUserInstructions(projectRoot);
        } else {
            throw new MojoExecutionException(
                    "The project was created but (some of) the requested extensions couldn't be added.");
        }
    }

    private void createGradleWrapper(QuarkusPlatformDescriptor platform, Path projectDirPath) {
        try {
            Files.createDirectories(projectDirPath.resolve("gradle/wrapper"));

            for (String filename : CreateUtils.GRADLE_WRAPPER_FILES) {
                byte[] fileContent = platform.loadResource(CreateUtils.GRADLE_WRAPPER_PATH + '/' + filename,
                        is -> {
                            byte[] buffer = new byte[is.available()];
                            is.read(buffer);
                            return buffer;
                        });
                final Path destination = projectDirPath.resolve(filename);
                Files.write(destination, fileContent);
            }

            projectDirPath.resolve("gradlew").toFile().setExecutable(true);
            projectDirPath.resolve("gradlew.bat").toFile().setExecutable(true);
        } catch (IOException e) {
            getLog().error("Unable to copy Gradle wrapper from platform descriptor", e);
        }
    }

    private void createMavenWrapper(File createdPomFile, Properties props) {
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

            setProxySystemPropertiesFromSession();

            executeMojo(
                    plugin(
                            groupId("io.takari"),
                            artifactId("maven"),
                            version(ToolsUtils.getMavenWrapperVersion(props))),
                    goal("wrapper"),
                    configuration(
                            element(name("maven"), ToolsUtils.getProposedMavenVersion(props))),
                    executionEnvironment(
                            newProject,
                            newSession,
                            pluginManager));
        } catch (Exception e) {
            // no reason to fail if the wrapper could not be created
            getLog().error("Unable to install the Maven wrapper (./mvnw) in the project", e);
        }
    }

    private void setProxySystemPropertiesFromSession() {
        List<Proxy> proxiesFromSession = session.getRequest().getProxies();
        // - takari maven uses https to download the maven wrapper
        // - don't do anything if proxy system property is already set
        if (!proxiesFromSession.isEmpty() && System.getProperty("https.proxyHost") == null) {

            // use the first active proxy for setting the system properties
            proxiesFromSession.stream()
                    .filter(Proxy::isActive)
                    .findFirst()
                    .ifPresent(proxy -> {
                        // note: a http proxy _is_ usable as https.proxyHost
                        System.setProperty("https.proxyHost", proxy.getHost());
                        System.setProperty("https.proxyPort", String.valueOf(proxy.getPort()));
                        if (proxy.getNonProxyHosts() != null) {
                            System.setProperty("http.nonProxyHosts", proxy.getNonProxyHosts());
                        }
                    });
        }
    }

    private void askTheUserForMissingValues() throws MojoExecutionException {

        // If the user has disabled the interactive mode or if the user has specified the artifactId, disable the
        // user interactions.
        if (!session.getRequest().isInteractiveMode() || shouldUseDefaults()) {
            if (StringUtils.isBlank(projectArtifactId)) {
                // we need to set it for the project directory
                projectArtifactId = DEFAULT_ARTIFACT_ID;
            }
            if (StringUtils.isBlank(projectGroupId)) {
                projectGroupId = DEFAULT_GROUP_ID;
            }
            if (StringUtils.isBlank(projectVersion)) {
                projectVersion = DEFAULT_VERSION;
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
                        DEFAULT_ARTIFACT_ID);
            }

            if (StringUtils.isBlank(projectVersion)) {
                projectVersion = prompter.promptWithDefaultValue("Set the project version",
                        DEFAULT_VERSION);
            }

            if (legacyCodegen) {
                if (StringUtils.isBlank(className)) {
                    // Ask the user if he want to create a resource
                    String answer = prompter.promptWithDefaultValue("Do you want to create a REST resource? (y/n)", "no");
                    if (isTrueOrYes(answer)) {
                        String defaultResourceName = projectGroupId.replace("-", ".")
                                .replace("_", ".") + ".HelloResource";
                        className = prompter.promptWithDefaultValue("Set the resource classname", defaultResourceName);
                        if (StringUtils.isBlank(path)) {
                            path = prompter.promptWithDefaultValue("Set the resource path ",
                                    CreateUtils.getDerivedPath(className));
                        }
                    } else {
                        className = null;
                        path = null;
                    }
                }
            } else {
                if (extensions.isEmpty()) {
                    extensions = Arrays
                            .stream(prompter.promptWithDefaultValue("What extensions do you wish to add (comma separated list)",
                                    DEFAULT_EXTENSIONS)
                                    .split(","))
                            .map(String::trim).filter(StringUtils::isNotEmpty)
                            .collect(Collectors.toSet());
                }
                String answer = prompter.promptWithDefaultValue(
                        "Do you want example code to get started (yes), or just an empty project (no)", "yes");
                noExamples = answer.startsWith("n");
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
        if (className != null) {
            className = sourceType.stripExtensionFrom(className);

            int idx = className.lastIndexOf('.');
            if (idx >= 0 && StringUtils.isBlank(packageName)) {
                // if it's a full qualified class name, we use the package name part (only if the packageName wasn't already defined)
                packageName = className.substring(0, idx);

                // And we strip it from the className
                className = className.substring(idx + 1);
            }

            if (StringUtils.isBlank(path)) {
                path = "/hello";
            } else if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }
        // if package name is empty, the groupId will be used as part of the CreateProject logic
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
                .a("mvn quarkus:dev")
                .reset()
                .toString());
        getLog().info(
                ansi().a("Your application will be accessible on ").bold().fg(Ansi.Color.CYAN).a("http://localhost:8080")
                        .reset().toString());
        getLog().info("========================================================================================");
        getLog().info("");
    }

}
