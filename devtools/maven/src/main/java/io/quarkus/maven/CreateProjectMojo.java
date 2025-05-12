package io.quarkus.maven;

import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.codestartLoadersBuilder;
import static org.fusesource.jansi.Ansi.ansi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.fusesource.jansi.Ansi;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.commands.CreateProjectHelper;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.components.Prompter;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.platform.tools.maven.MojoMessageWriter;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;

/**
 * This goal helps in setting up Quarkus Maven project with quarkus-maven-plugin, with sensible defaults
 */
@Mojo(name = "create", requiresProject = false)
public class CreateProjectMojo extends AbstractMojo {
    static final String BAD_IDENTIFIER = "The specified %s identifier (%s) contains invalid characters. Valid characters are alphanumeric characters (A-Za-z0-9), underscores, dashes and dots.";
    static final Pattern OK_ID = Pattern.compile("[0-9A-Za-z_.-]+");

    private static final String DEFAULT_GROUP_ID = "org.acme";
    private static final String DEFAULT_ARTIFACT_ID = "code-with-quarkus";
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_EXTENSIONS = "rest";

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "projectGroupId")
    private String projectGroupId;

    @Parameter(property = "projectArtifactId")
    private String projectArtifactId;

    @Parameter(property = "projectVersion")
    private String projectVersion;

    @Parameter(property = "projectName")
    private String projectName;

    @Parameter(property = "projectDescription")
    private String projectDescription;

    /**
     * When true, do not include any code in the generated Quarkus project.
     */
    @Parameter(property = "noCode", defaultValue = "false")
    private boolean noCode;

    @Parameter(property = "example")
    private String example;

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

    /**
     * Version of Java used to build the project.
     */
    @Parameter(property = "javaVersion")
    private String javaVersion;

    /**
     * The {@link #path} will define the REST path of the generated code when picking only one of those extensions REST,
     * RESTEasy Classic and Spring-Web.
     * <br />
     * If more than one of those extensions are picked, this parameter will be ignored.
     * <br />
     * More info: https://github.com/quarkusio/quarkus/issues/14437
     * <br />
     * {@code className}
     */
    @Parameter(property = "path")
    private String path;

    /**
     * The {@link #className} will define the generated class names when picking only one of those extensions REST,
     * RESTEasy Classic and Spring-Web.
     * <br />
     * If more than one of those extensions are picked, then only the package name part will be used as {@link #packageName}
     * <br />
     * More info: https://github.com/quarkusio/quarkus/issues/14437
     * <br />
     * By default, the {@link #projectGroupId} is used as package for generated classes (you can also use {@link #packageName}
     * to have them different).
     * <br />
     * {@code className}
     */
    @Parameter(property = "className")
    private String className;

    /**
     * Set the package name of the generated classes.
     * <br />
     * If not set, {@link #projectGroupId} will be used as {@link #packageName}
     * <p>
     * {@code packageName}
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
    private MavenVersionEnforcer mavenVersionEnforcer;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private ProjectBuilder projectBuilder;

    @Component
    private RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepoManager;

    @Parameter(property = "appConfig")
    private String appConfig;

    @Parameter(property = "data")
    private String data;

    @Component
    QuarkusWorkspaceProvider workspaceProvider;

    @Override
    public void execute() throws MojoExecutionException {

        // We detect the Maven version during the project generation to indicate the user immediately that the installed
        // version may not be supported.
        mavenVersionEnforcer.ensureMavenVersion(getLog(), session);
        try {
            Files.createDirectories(outputDirectory.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create directory " + outputDirectory, e);
        }

        final MavenArtifactResolver mvn;
        try {
            mvn = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(
                            getLog().isDebugEnabled() ? repoSession : MojoUtils.muteTransferListener(repoSession))
                    .setRemoteRepositories(repos)
                    .setRemoteRepositoryManager(remoteRepoManager)
                    .build();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
        }

        final MojoMessageWriter log = new MojoMessageWriter(getLog());
        ExtensionCatalogResolver catalogResolver;
        try {
            catalogResolver = QuarkusProjectHelper.isRegistryClientEnabled()
                    ? QuarkusProjectHelper.getCatalogResolver(mvn, log)
                    : ExtensionCatalogResolver.empty();
        } catch (RegistryResolutionException e) {
            // fall back to the default platform
            catalogResolver = ExtensionCatalogResolver.empty();
        }
        ExtensionCatalog catalog = resolveExtensionsCatalog(this, bomGroupId, bomArtifactId, bomVersion, catalogResolver,
                mvn, log);

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
                        "You are trying to create a Maven project in a directory that contains only Gradle build files.");
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
        if (!DEFAULT_ARTIFACT_ID.equals(projectArtifactId) && !OK_ID.matcher(projectArtifactId).matches()) {
            throw new MojoExecutionException(String.format(BAD_IDENTIFIER, "artifactId", projectArtifactId));
        }
        if (!DEFAULT_GROUP_ID.equals(projectGroupId) && !OK_ID.matcher(projectGroupId).matches()) {
            throw new MojoExecutionException(String.format(BAD_IDENTIFIER, "groupId", projectGroupId));
        }

        projectRoot = new File(outputDirectory, projectArtifactId);
        if (projectRoot.exists()) {
            throw new MojoExecutionException("Unable to create the project, " +
                    "the directory " + projectRoot.getAbsolutePath() + " already exists");
        }

        boolean success;
        final Path projectDirPath = projectRoot.toPath();
        try {
            extensions = CreateProjectHelper.sanitizeExtensions(extensions);
            catalog = CreateProjectHelper.completeCatalog(catalog, extensions, mvn);
            sanitizeOptions();

            final List<ResourceLoader> codestartsResourceLoader = codestartLoadersBuilder(log)
                    .catalog(catalog)
                    .artifactResolver(mvn)
                    .build();
            QuarkusProject newProject = QuarkusProject.of(projectDirPath, catalog,
                    codestartsResourceLoader, log, buildToolEnum, new JavaVersion(javaVersion));
            final CreateProject createProject = new CreateProject(newProject)
                    .groupId(projectGroupId)
                    .artifactId(projectArtifactId)
                    .version(projectVersion)
                    .name(projectName)
                    .description(projectDescription)
                    .javaVersion(javaVersion)
                    .resourceClassName(className)
                    .packageName(packageName)
                    .extensions(extensions)
                    .resourcePath(path)
                    .example(example)
                    .noCode(noCode)
                    .appConfig(appConfig)
                    .data(data);

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
                MojoUtils.writeFormatted(parentPomModel, pom);
                MojoUtils.writeFormatted(subModulePomModel, subModulePomFile);
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

    static ExtensionCatalog resolveExtensionsCatalog(AbstractMojo mojo, String groupId, String artifactId, String version,
            ExtensionCatalogResolver catalogResolver, MavenArtifactResolver artifactResolver, MessageWriter log)
            throws MojoExecutionException {

        if (catalogResolver.hasRegistries()) {
            try {
                return isBlank(groupId) && isBlank(artifactId) && isBlank(version)
                        ? catalogResolver.resolveExtensionCatalog()
                        : catalogResolver.resolveExtensionCatalog(List.of(
                                ArtifactCoords.pom(getPlatformGroupId(mojo, groupId), getPlatformArtifactId(artifactId),
                                        getPlatformVersion(mojo, version))));
            } catch (RegistryResolutionException e) {
                log.warn(e.getLocalizedMessage());
                mojo.getLog().debug(e);
            }
        }
        return resolveExtensionCatalogDirectly(mojo, groupId, artifactId, version, catalogResolver, artifactResolver, log);
    }

    private static ExtensionCatalog resolveExtensionCatalogDirectly(AbstractMojo mojo, String groupId, String artifactId,
            String version,
            ExtensionCatalogResolver catalogResolver, MavenArtifactResolver artifactResolver, MessageWriter log) {
        groupId = getPlatformGroupId(mojo, groupId);
        artifactId = getPlatformArtifactId(artifactId);
        version = getPlatformVersion(mojo, version);
        final ExtensionCatalog catalog = ToolsUtils.resolvePlatformDescriptorDirectly(groupId, artifactId, version,
                artifactResolver, log);

        final StringBuilder buf = new StringBuilder();
        buf.append("The extension catalog will be narrowed to the ").append(groupId).append(":").append(artifactId)
                .append(":").append(version).append(" platform release.");
        if (!QuarkusProjectHelper.isRegistryClientEnabled()) {
            buf.append(
                    " To enable the complete Quarkiverse extension catalog along with the latest recommended platform releases, please, make sure the extension registry client is enabled.");
        }
        log.warn(buf.toString());
        return catalog;
    }

    private void askTheUserForMissingValues() throws MojoExecutionException {

        // If the user has disabled the interactive mode or if the user has specified the artifactId, disable the
        // user interactions.
        if (!session.getRequest().isInteractiveMode() || shouldUseDefaults()) {
            setProperDefaults();
            return;
        }

        try {
            final Prompter prompter = new Prompter();
            if (isBlank(projectGroupId)) {
                prompter.addPrompt("Set the project groupId: ", DEFAULT_GROUP_ID, input -> projectGroupId = input);
            }

            if (isBlank(projectArtifactId)) {
                prompter.addPrompt("Set the project artifactId: ", DEFAULT_ARTIFACT_ID, input -> projectArtifactId = input);
            }

            if (isBlank(projectVersion)) {
                prompter.addPrompt("Set the project version: ", DEFAULT_VERSION, input -> projectVersion = input);
            }

            if (!noCode && isBlank(example)) {
                if (extensions.isEmpty()) {
                    prompter.addPrompt("What extensions do you wish to add (comma separated list): ", DEFAULT_EXTENSIONS,
                            input -> extensions = Arrays
                                    .stream(input.split(","))
                                    .map(String::trim).filter(Predicate.not(String::isEmpty)).collect(Collectors.toSet()));
                }
                prompter.addPrompt("Would you like some code to start (yes), or just an empty Quarkus project (no): ", "yes",
                        input -> noCode = input.startsWith("n"));

                prompter.collectInput();
            } else {
                setProperDefaults();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to get user input", e);
        }
    }

    private void setProperDefaults() {
        if (isBlank(projectArtifactId)) {
            // we need to set it for the project directory
            projectArtifactId = DEFAULT_ARTIFACT_ID;
        }
        if (isBlank(projectGroupId)) {
            projectGroupId = DEFAULT_GROUP_ID;
        }
        if (isBlank(projectVersion)) {
            projectVersion = DEFAULT_VERSION;
        }
    }

    private boolean shouldUseDefaults() {
        // Must be called before user input
        return projectArtifactId != null;

    }

    private void sanitizeOptions() {
        if (className != null) {
            className = className.replaceAll("\\.(java|kotlin|scala)$", "");
            int idx = className.lastIndexOf('.');
            if (idx >= 0 && isBlank(packageName)) {
                // if it's a full qualified class name, we use the package name part (only if the packageName wasn't already defined)
                packageName = className.substring(0, idx);

                // And we strip it from the className
                className = className.substring(idx + 1);
            }

            if (isBlank(path)) {
                path = "/hello";
            } else if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }
        // if package name is empty, the groupId will be used as part of the CreateProject logic
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

    public static String getPlatformVersion(AbstractMojo mojo, String version) {
        return isBlank(version) ? getPluginVersion(mojo) : version;
    }

    public static String getPlatformArtifactId(String artifactId) {
        return isBlank(artifactId) ? "quarkus-bom" : artifactId;
    }

    public static String getPlatformGroupId(AbstractMojo mojo, String groupId) {
        return isBlank(groupId) ? getPluginGroupId(mojo) : groupId;
    }

    private static String getPluginGroupId(AbstractMojo mojo) {
        return getPluginDescriptor(mojo).getGroupId();
    }

    private static String getPluginVersion(AbstractMojo mojo) {
        return getPluginDescriptor(mojo).getVersion();
    }

    private static PluginDescriptor getPluginDescriptor(AbstractMojo mojo) {
        return (PluginDescriptor) mojo.getPluginContext().get("pluginDescriptor");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
