package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.MessageWriter;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import io.quarkus.platform.tools.maven.MojoMessageWriter;

public abstract class BuildFileMojoBase extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Component
    protected RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    protected RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    protected List<RemoteRepository> repos;

    @Parameter(property = "bomGroupId", defaultValue = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID)
    private String bomGroupId;

    @Parameter(property = "bomArtifactId", required = false)
    private String bomArtifactId;

    @Parameter(property = "bomVersion", required = false)
    private String bomVersion;

    @Override
    public void execute() throws MojoExecutionException {

        // Validate Mojo parameters
        validateParameters();

        // Resolve and setup the platform descriptor
        final MavenArtifactResolver mvn;
        try {
            mvn = MavenArtifactResolver.builder().setRepositorySystem(repoSystem).setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos).build();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize maven artifact resolver", e);
        }

        final MessageWriter log = new MojoMessageWriter(getLog());

        BuildFile buildFile = null;
        try {
            if (project.getFile() != null) {
                // Maven project
                @SuppressWarnings("resource")
                final MavenBuildFile mvnBuild = new MavenBuildFile(new FileProjectWriter(project.getBasedir()));
                buildFile = mvnBuild;

                if (QuarkusPlatformConfig.hasGlobalDefault()) {
                    getLog().warn("The Quarkus platform default configuration has already been initialized.");
                } else {
                    Artifact descrArtifact = null;
                    for (Dependency dep : mvnBuild.getManagedDependencies()) {
                        if (!dep.getScope().equals("import") && !dep.getType().equals("pom")) {
                            continue;
                        }
                        // We don't know which BOM is the platform one, so we are trying every BOM here
                        final String bomVersion = resolveValue(dep.getVersion(), buildFile);
                        final String bomGroupId = resolveValue(dep.getGroupId(), buildFile);
                        final String bomArtifactId = resolveValue(dep.getArtifactId(), buildFile);
                        if (bomVersion == null || bomGroupId == null || bomArtifactId == null) {
                            continue;
                        }

                        Artifact jsonArtifact = new DefaultArtifact(bomGroupId, bomArtifactId, null, "json", bomVersion);
                        try {
                            jsonArtifact = mvn.resolve(jsonArtifact).getArtifact();
                        } catch (Exception e) {
                            log.debug("Failed to resolve JSON descriptor as %s", jsonArtifact);
                            jsonArtifact = new DefaultArtifact(bomGroupId, bomArtifactId + "-descriptor-json", null, "json",
                                    bomVersion);
                            try {
                                jsonArtifact = mvn.resolve(jsonArtifact).getArtifact();
                            } catch (Exception e1) {
                                getLog().debug("Failed to resolve JSON descriptor as " + jsonArtifact);
                                continue;
                            }
                        }
                        descrArtifact = jsonArtifact;
                        break;
                    }
                    if (descrArtifact != null) {
                        log.debug("Quarkus platform JSON descriptor resolved from %s", descrArtifact);
                        final QuarkusPlatformDescriptor platform = QuarkusJsonPlatformDescriptorResolver.newInstance()
                                .setArtifactResolver(new BootstrapAppModelResolver(mvn))
                                .setMessageWriter(log)
                                .resolveFromJson(descrArtifact.getFile().toPath());
                        QuarkusPlatformConfig.defaultConfigBuilder()
                                .setPlatformDescriptor(platform)
                                .build();
                    }
                }
            } else if (new File(project.getBasedir(), "build.gradle").exists()
                    || new File(project.getBasedir(), "build.gradle.kts").exists()) {
                // Gradle project
                buildFile = new GradleBuildFile(new FileProjectWriter(project.getBasedir()));
            }

            if (!QuarkusPlatformConfig.hasGlobalDefault()) {
                CreateUtils.setGlobalPlatformDescriptor(bomGroupId, bomArtifactId, bomVersion, mvn, getLog());
            }

            doExecute(buildFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to initialize project reading tools", e);
        } finally {
            if (buildFile != null) {
                try {
                    buildFile.close();
                } catch (IOException e) {
                    log.debug("Failed to close %s", buildFile, e);
                }
            }
        }
    }

    protected void validateParameters() throws MojoExecutionException {
    }

    protected abstract void doExecute(BuildFile buildFile) throws MojoExecutionException;

    private String resolveValue(String expr, BuildFile buildFile) throws IOException {
        if (expr.startsWith("${") && expr.endsWith("}")) {
            final String v = buildFile.getProperty(expr.substring(2, expr.length() - 1));
            if (v == null) {
                getLog().debug("Failed to resolve version of " + v);
            }
            return v;
        }
        return expr;
    }
}
