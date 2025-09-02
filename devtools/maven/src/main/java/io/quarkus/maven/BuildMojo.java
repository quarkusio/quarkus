package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.analytics.dto.segment.TrackEventType;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Builds the Quarkus application.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class BuildMojo extends QuarkusBootstrapMojo {

    @Component
    MavenProjectHelper projectHelper;

    @Component
    BuildAnalyticsProvider analyticsProvider;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true, required = true)
    List<RemoteRepository> pluginRepos;

    /**
     * The directory for generated source files.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources")
    File generatedSourcesDirectory;

    /**
     * Skips the execution of this mojo
     */
    @Parameter(defaultValue = "false", property = "quarkus.build.skip")
    boolean skip = false;

    /**
     * When the building an Uber JAR, the default JAR is renamed by adding {@code .original} suffix.
     * Enabling this property will disable the renaming of the original JAR.
     */
    @Deprecated
    @Parameter(property = "skipOriginalJarRename")
    boolean skipOriginalJarRename;

    /**
     * Whether to replace the original JAR with the Uber runner JAR as the main project artifact
     */
    @Parameter(property = "attachRunnerAsMainArtifact", required = false)
    boolean attachRunnerAsMainArtifact;

    /**
     * Whether to attach SBOMs generated for Uber JARs as project artifacts
     */
    @Parameter(property = "attachSboms")
    boolean attachSboms = true;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    File buildDirectory;

    /**
     * The list of system properties defined for the plugin.
     */
    @Parameter
    Map<String, String> systemProperties = Collections.emptyMap();

    @Override
    protected boolean beforeExecute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping Quarkus build");
            return false;
        }
        if (mavenProject().getPackaging().equals(ArtifactCoords.TYPE_POM)) {
            getLog().info("Type of the artifact is POM, skipping build goal");
            return false;
        }
        if (!mavenProject().getArtifact().getArtifactHandler().getExtension().equals(ArtifactCoords.TYPE_JAR)) {
            throw new MojoExecutionException(
                    "The project artifact's extension is '" + mavenProject().getArtifact().getArtifactHandler().getExtension()
                            + "' while this goal expects it be 'jar'");
        }
        return true;
    }

    @Override
    protected void doExecute() throws MojoExecutionException {
        try {
            Set<String> propertiesToClear = new HashSet<>();

            // Add the system properties of the plugin to the system properties
            // if and only if they are not already set.
            for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }

                String key = entry.getKey();
                if (System.getProperty(key) == null) {
                    System.setProperty(key, entry.getValue());
                    propertiesToClear.add(key);
                }
            }

            if (setNativeEnabledIfNativeProfileEnabled()) {
                propertiesToClear.add("quarkus.native.enabled");
            }

            if (!propertiesToClear.isEmpty() && mavenSession().getRequest().getDegreeOfConcurrency() > 1) {
                getLog().warn("*****************************************************************");
                getLog().warn("* Your build is requesting parallel execution, but the project  *");
                getLog().warn("* relies on System properties at build time which could cause   *");
                getLog().warn("* race condition issues thus unpredictable build results.       *");
                getLog().warn("* Please avoid using System properties or avoid enabling        *");
                getLog().warn("* parallel execution                                            *");
                getLog().warn("*****************************************************************");
            }
            try (CuratedApplication curatedApplication = bootstrapApplication()) {
                AugmentAction action = curatedApplication.createAugmentor();
                AugmentResult result = action.createProductionApplication();
                analyticsProvider.sendAnalytics(
                        TrackEventType.BUILD,
                        curatedApplication.getApplicationModel(),
                        result.getGraalVMInfo(),
                        buildDirectory);
                Artifact original = mavenProject().getArtifact();
                if (result.getJar() != null) {

                    final boolean uberJarWithSuffix = result.getJar().isUberJar()
                            && result.getJar().getOriginalArtifact() != null
                            && !result.getJar().getOriginalArtifact().equals(result.getJar().getPath());
                    if (!skipOriginalJarRename && uberJarWithSuffix
                            && result.getJar().getOriginalArtifact() != null) {
                        final Path standardJar = result.getJar().getOriginalArtifact();
                        if (Files.exists(standardJar)) {
                            final Path renamedOriginal = standardJar.getParent().toAbsolutePath()
                                    .resolve(standardJar.getFileName() + ".original");
                            try {
                                IoUtils.recursiveDelete(renamedOriginal);
                                Files.move(standardJar, renamedOriginal);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            // unless we point to the renamed file the install plugin will fail
                            original.setFile(renamedOriginal.toFile());
                        }
                    }
                    if (uberJarWithSuffix) {
                        if (attachRunnerAsMainArtifact || result.getJar().getClassifier().isEmpty()) {
                            original.setFile(result.getJar().getPath().toFile());
                        } else {
                            projectHelper.attachArtifact(mavenProject(), result.getJar().getPath().toFile(),
                                    result.getJar().getClassifier());
                        }
                    }
                    if (attachSboms && result.getJar().isUberJar() && !result.getJar().getSboms().isEmpty()) {
                        for (var sbom : result.getJar().getSboms()) {
                            projectHelper.attachArtifact(mavenProject(), sbom.getFormat(), sbom.getClassifier(),
                                    sbom.getSbomFile().toFile());
                        }
                    }
                }
            } finally {
                // Clear all the system properties set by the plugin
                propertiesToClear.forEach(System::clearProperty);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to build quarkus application", e);
        }
    }

    @Override
    public void setLog(Log log) {
        super.setLog(log);
        MojoLogger.delegate = log;
    }
}
