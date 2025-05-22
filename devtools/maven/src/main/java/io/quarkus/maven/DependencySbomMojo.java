package io.quarkus.maven;

import java.io.File;
import java.nio.file.*;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.EffectiveModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.cyclonedx.generator.CycloneDxSbomGenerator;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.sbom.ApplicationManifest;
import io.quarkus.sbom.ApplicationManifestConfig;

/**
 * Quarkus application SBOM generator
 */
@Mojo(name = "dependency-sbom", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class DependencySbomMojo extends AbstractMojo {

    @Component
    QuarkusWorkspaceProvider workspaceProvider;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    MavenSession session;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    List<RemoteRepository> repos;

    /**
     * Whether to skip the execution of the goal
     */
    @Parameter(defaultValue = "false", property = "quarkus.dependency.sbom.skip")
    boolean skip = false;

    /**
     * Target launch mode corresponding to {@link io.quarkus.runtime.LaunchMode} for which the SBOM should be built.
     * {@code io.quarkus.runtime.LaunchMode.NORMAL} is the default.
     */
    @Parameter(property = "quarkus.dependency.sbom.mode", defaultValue = "prod")
    String mode;

    /**
     * CycloneDX BOM format. Allowed values are {@code json} and {@code xml}. The default is {@code json}.
     */
    @Parameter(property = "quarkus.dependency.sbom.format", defaultValue = "json")
    String format;

    /**
     * File to store the SBOM in. If not configured, the SBOM will be stored in the ${project.build.directory} directory.
     */
    @Parameter(property = "quarkus.dependency.sbom.output-file")
    File outputFile;

    /**
     * Whether to include license text in the generated SBOM. The default is {@code false}
     */
    @Parameter(property = "quarkus.dependency.sbom.include-license-text", defaultValue = "false")
    boolean includeLicenseText;

    /**
     * CycloneDX BOM schema version
     */
    @Parameter(property = "quarkus.dependency.sbom.schema-version")
    String schemaVersion;

    /**
     * Whether to limit application dependencies to only those that are included in the runtime
     */
    @Parameter(property = "quarkus.dependency.sbom.runtime-only")
    boolean runtimeOnly;

    protected MavenArtifactResolver resolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping config dump");
            return;
        }
        final Path outputFilePath = getSbomFile().toPath();
        CycloneDxSbomGenerator.newInstance()
                .setManifest(ApplicationManifest.fromConfig(
                        ApplicationManifestConfig.builder()
                                .setApplicationModel(resolveApplicationModel())
                                .build()))
                .setOutputFile(outputFilePath)
                .setFormat(format)
                .setEffectiveModelResolver(EffectiveModelResolver.of(getResolver()))
                .setSchemaVersion(schemaVersion)
                .setIncludeLicenseText(includeLicenseText)
                .generate();
        getLog().info("The SBOM has been saved in " + outputFilePath);
    }

    private ApplicationModel resolveApplicationModel()
            throws MojoExecutionException {
        final ArtifactCoords appArtifact = ArtifactCoords.pom(project.getGroupId(), project.getArtifactId(),
                project.getVersion());
        final BootstrapAppModelResolver modelResolver;
        try {
            modelResolver = new BootstrapAppModelResolver(getResolver())
                    .setRuntimeModelOnly(runtimeOnly);
            if (mode != null) {
                if (mode.equalsIgnoreCase("test")) {
                    modelResolver.setTest(true);
                } else if (mode.equalsIgnoreCase("dev") || mode.equalsIgnoreCase("development")) {
                    modelResolver.setDevMode(true);
                } else if (mode.equalsIgnoreCase("prod") || mode.isEmpty()) {
                    // ignore, that's the default
                } else {
                    throw new MojoExecutionException(
                            "Parameter 'mode' was set to '" + mode + "' while expected one of 'dev', 'test' or 'prod'");
                }
            }
            modelResolver.setLegacyModelResolver(BootstrapAppModelResolver.isLegacyModelResolver(project.getProperties()));
            return modelResolver.resolveModel(appArtifact);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
    }

    private String getSbomFilename() {
        var a = project.getArtifact();
        var sb = new StringBuilder().append(a.getArtifactId()).append("-").append(a.getVersion()).append("-");
        if (!"prod".equalsIgnoreCase(mode)) {
            sb.append(mode).append("-");
        }
        return sb.append("dependency-cyclonedx").append(".").append(format).toString();
    }

    private File getSbomFile() {
        var f = outputFile;
        if (f == null) {
            f = new File(project.getBuild().getDirectory(), getSbomFilename());
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("SBOM will be stored in " + f);
        }
        return f;
    }

    protected MavenArtifactResolver getResolver() {
        if (resolver == null) {
            resolver = workspaceProvider.createArtifactResolver(BootstrapMavenContext.config()
                    .setUserSettings(session.getRequest().getUserSettingsFile())
                    // The system needs to be initialized with the bootstrap model builder to properly interpolate system properties set on the command line
                    // e.g. -Dquarkus.platform.version=xxx
                    //.setRepositorySystem(repoSystem)
                    // The session should be initialized with the loaded workspace
                    //.setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    // To support multi-module projects that haven't been installed
                    .setPreferPomsFromWorkspace(true));
        }
        return resolver;
    }
}
