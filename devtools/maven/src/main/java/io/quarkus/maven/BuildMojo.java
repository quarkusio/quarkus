package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;

/**
 * Build the application.
 * <p>
 * You can build a native application runner with {@code native-image}
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class BuildMojo extends AbstractMojo {

    protected static final String QUARKUS_PACKAGE_UBER_JAR = "quarkus.package.uber-jar";
    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of artifacts and their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true, required = true)
    private List<RemoteRepository> pluginRepos;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    @Deprecated
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The directory for generated source files.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources")
    private File generatedSourcesDirectory;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Parameter(defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(property = "uberJar", defaultValue = "false")
    private boolean uberJar;

    /**
     * When using the uberJar option, this array specifies entries that should
     * be excluded from the final jar. The entries are relative to the root of
     * the file. An example of this configuration could be:
     * <code><pre>
     * &#x3C;configuration&#x3E;
     *   &#x3C;uberJar&#x3E;true&#x3C;/uberJar&#x3E;
     *   &#x3C;ignoredEntries&#x3E;
     *     &#x3C;ignoredEntry&#x3E;META-INF/BC2048KE.SF&#x3C;/ignoredEntry&#x3E;
     *     &#x3C;ignoredEntry&#x3E;META-INF/BC2048KE.DSA&#x3C;/ignoredEntry&#x3E;
     *     &#x3C;ignoredEntry&#x3E;META-INF/BC1024KE.SF&#x3C;/ignoredEntry&#x3E;
     *     &#x3C;ignoredEntry&#x3E;META-INF/BC1024KE.DSA&#x3C;/ignoredEntry&#x3E;
     *   &#x3C;/ignoredEntries&#x3E;
     * &#x3C;/configuration&#x3E;
     * </pre></code>
     */
    @Parameter(property = "ignoredEntries")
    private String[] ignoredEntries;

    /** Skip the execution of this mojo */
    @Parameter(defaultValue = "false", property = "quarkus.build.skip")
    private boolean skip = false;

    public BuildMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping Quarkus build");
            return;
        }
        if (project.getPackaging().equals("pom")) {
            getLog().info("Type of the artifact is POM, skipping build goal");
            return;
        }
        if (!project.getArtifact().getArtifactHandler().getExtension().equals("jar")) {
            throw new MojoExecutionException(
                    "The project artifact's extension is '" + project.getArtifact().getArtifactHandler().getExtension()
                            + "' while this goal expects it be 'jar'");
        }

        boolean clear = false;
        try {

            final Properties projectProperties = project.getProperties();
            final Properties effectiveProperties = new Properties();
            // quarkus. properties > ignoredEntries in pom.xml
            if (ignoredEntries != null && ignoredEntries.length > 0) {
                String joinedEntries = String.join(",", ignoredEntries);
                effectiveProperties.setProperty("quarkus.package.user-configured-ignored-entries", joinedEntries);
            }
            for (String name : projectProperties.stringPropertyNames()) {
                if (name.startsWith("quarkus.")) {
                    effectiveProperties.setProperty(name, projectProperties.getProperty(name));
                }
            }
            if (uberJar && System.getProperty(QUARKUS_PACKAGE_UBER_JAR) == null) {
                System.setProperty(QUARKUS_PACKAGE_UBER_JAR, "true");
                clear = true;
            }
            effectiveProperties.putIfAbsent("quarkus.application.name", project.getArtifactId());
            effectiveProperties.putIfAbsent("quarkus.application.version", project.getVersion());

            MavenArtifactResolver resolver = MavenArtifactResolver.builder()
                    .setWorkspaceDiscovery(false)
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .build();

            final Artifact projectArtifact = project.getArtifact();
            final AppArtifact appArtifact = new AppArtifact(projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                    projectArtifact.getClassifier(), projectArtifact.getArtifactHandler().getExtension(),
                    projectArtifact.getVersion());

            File projectFile = projectArtifact.getFile();
            if (projectFile == null) {
                projectFile = new File(project.getBuild().getOutputDirectory());
                if (!projectFile.exists()) {
                    if (hasSources(project)) {
                        throw new MojoExecutionException("Project " + project.getArtifact() + " has not been compiled yet");
                    }
                    if (!projectFile.mkdirs()) {
                        throw new MojoExecutionException("Failed to create the output dir " + projectFile);
                    }
                }
            }
            appArtifact.setPaths(PathsCollection.of(projectFile.toPath()));

            QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setAppArtifact(appArtifact)
                    .setMavenArtifactResolver(resolver)
                    .setIsolateDeployment(true)
                    .setBaseClassLoader(BuildMojo.class.getClassLoader())
                    .setBuildSystemProperties(effectiveProperties)
                    .setLocalProjectDiscovery(false)
                    .setProjectRoot(project.getBasedir().toPath())
                    .setBaseName(finalName)
                    .setTargetDirectory(buildDir.toPath());

            for (MavenProject project : project.getCollectedProjects()) {
                builder.addLocalArtifact(new AppArtifactKey(project.getGroupId(), project.getArtifactId(), null,
                        project.getArtifact().getArtifactHandler().getExtension()));
            }

            try (CuratedApplication curatedApplication = builder
                    .build().bootstrap()) {

                AugmentAction action = curatedApplication.createAugmentor();
                AugmentResult result = action.createProductionApplication();

                Artifact original = project.getArtifact();
                if (result.getJar() != null) {

                    if (result.getJar().isUberJar() && result.getJar().getOriginalArtifact() != null) {
                        final Path standardJar = curatedApplication.getAppModel().getAppArtifact().getPaths().getSinglePath();
                        if (Files.exists(standardJar)) {
                            try {
                                Files.deleteIfExists(result.getJar().getOriginalArtifact());
                                Files.move(standardJar, result.getJar().getOriginalArtifact());
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            original.setFile(result.getJar().getOriginalArtifact().toFile());
                        }
                    }
                    if (result.getJar().isUberJar()) {
                        projectHelper.attachArtifact(project, result.getJar().getPath().toFile(),
                                result.getJar().getClassifier());
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to build quarkus application", e);
        } finally {
            if (clear) {
                System.clearProperty(QUARKUS_PACKAGE_UBER_JAR);
            }
        }
    }

    private static boolean hasSources(MavenProject project) {
        if (new File(project.getBuild().getSourceDirectory()).exists()) {
            return true;
        }
        for (Resource r : project.getBuild().getResources()) {
            if (new File(r.getDirectory()).exists()) {
                return true;
            }
        }
        return false;
    }
}
