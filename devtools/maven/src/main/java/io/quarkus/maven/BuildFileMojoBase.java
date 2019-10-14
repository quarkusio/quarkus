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

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public abstract class BuildFileMojoBase extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Component
    protected RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    protected RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    protected List<RemoteRepository> repos;

    @Override
    public void execute() throws MojoExecutionException {

        validateParameters();

        final MavenArtifactResolver mvn;
        try {
            mvn = MavenArtifactResolver.builder().setRepositorySystem(repoSystem).setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos).build();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize maven artifact resolver", e);
        }

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
                        String bomVersion = dep.getVersion();
                        if (bomVersion.startsWith("${") && bomVersion.endsWith("}")) {
                            final String prop = bomVersion.substring(2, bomVersion.length() - 1);
                            bomVersion = mvnBuild.getProperty(prop);
                            if (bomVersion == null) {
                                getLog().debug("Failed to resolve version of " + dep);
                                continue;
                            }
                        }
                        Artifact jsonArtifact = new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(),
                                "json", bomVersion);
                        try {
                            jsonArtifact = mvn.resolve(jsonArtifact).getArtifact();
                        } catch (Exception e) {
                            getLog().debug("Failed to resolve JSON descriptor as " + jsonArtifact);
                            jsonArtifact = new DefaultArtifact(dep.getGroupId(), dep.getArtifactId() + "-descriptor-json",
                                    dep.getClassifier(), "json", bomVersion);
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
                        getLog().debug("Quarkus platform JSON descriptor artifact: " + descrArtifact);
                        CreateUtils.setupQuarkusJsonPlatformDescriptor(mvn, descrArtifact, getLog());
                    }
                }
            } else if (new File(project.getBasedir(), "build.gradle").exists()
                    || new File(project.getBasedir(), "build.gradle.kts").exists()) {
                // Gradle project
                buildFile = new GradleBuildFile(new FileProjectWriter(project.getBasedir()));
            }

            if (!QuarkusPlatformConfig.hasGlobalDefault()) {
                CreateUtils.setupQuarkusJsonPlatformDescriptor(mvn,
                        new DefaultArtifact(CreateUtils.DEFAULT_PLATFORM_GROUP_ID, CreateUtils.DEFAULT_PLATFORM_ARTIFACT_ID,
                                null, "json", CreateUtils.resolvePluginInfo(BuildFileMojoBase.class).getVersion()),
                        getLog());
            }

            doExecute(buildFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to initialize project reading tools", e);
        } finally {
            if (buildFile != null) {
                try {
                    buildFile.close();
                } catch (IOException e) {
                    getLog().debug("Failed to close " + buildFile, e);
                }
            }
        }
    }

    protected void validateParameters() throws MojoExecutionException {
    }

    protected abstract void doExecute(BuildFile buildFile) throws MojoExecutionException;
}
