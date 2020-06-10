package io.quarkus.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
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
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.platform.descriptor.CombinedQuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.MessageWriter;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.maven.MojoMessageWriter;

public abstract class QuarkusProjectMojoBase extends AbstractMojo {

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

        final MessageWriter log = new MojoMessageWriter(getLog());
        final Path projectFolderPath = project.getBasedir().toPath();
        final BuildTool buildTool = QuarkusProject.resolveExistingProjectBuildTool(projectFolderPath);
        final QuarkusPlatformDescriptor platformDescriptor = resolvePlatformDescriptor(log);

        doExecute(QuarkusProject.of(project.getBasedir().toPath(), platformDescriptor, buildTool), log);
    }

    private QuarkusPlatformDescriptor resolvePlatformDescriptor(final MessageWriter log) throws MojoExecutionException {
        // Resolve and setup the platform descriptor
        try {
            final MavenArtifactResolver mvn = MavenArtifactResolver.builder().setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos).build();
            if (project.getFile() != null) {
                final List<Artifact> descrArtifactList = new ArrayList<>(2);
                for (Dependency dep : getManagedDependencies()) {
                    if ((dep.getScope() == null || !dep.getScope().equals("import"))
                            && (dep.getType() == null || !dep.getType().equals("pom"))) {
                        continue;
                    }
                    // We don't know which BOM is the platform one, so we are trying every BOM here
                    final String bomVersion = resolveValue(dep.getVersion());
                    final String bomGroupId = resolveValue(dep.getGroupId());
                    final String bomArtifactId = resolveValue(dep.getArtifactId());
                    if (bomVersion == null || bomGroupId == null || bomArtifactId == null) {
                        continue;
                    }

                    final Artifact jsonArtifact = resolveJsonOrNull(mvn, bomGroupId, bomArtifactId, bomVersion);
                    if (jsonArtifact != null) {
                        descrArtifactList.add(jsonArtifact);
                    }
                }
                if (!descrArtifactList.isEmpty()) {
                    if (descrArtifactList.size() == 1) {
                        return loadPlatformDescriptor(mvn, log, descrArtifactList.get(0));
                    } else {
                        final CombinedQuarkusPlatformDescriptor.Builder builder = CombinedQuarkusPlatformDescriptor.builder();
                        for (Artifact descrArtifact : descrArtifactList) {
                            builder.addPlatform(loadPlatformDescriptor(mvn, log, descrArtifact));
                        }
                        return builder.build();
                    }
                }
            }
            return CreateUtils.resolvePlatformDescriptor(bomGroupId, bomArtifactId, bomVersion, mvn, getLog());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize maven artifact resolver", e);
        }
    }

    private QuarkusPlatformDescriptor loadPlatformDescriptor(final MavenArtifactResolver mvn, final MessageWriter log,
            Artifact descrArtifact) {
        return QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setArtifactResolver(new BootstrapAppModelResolver(mvn))
                .setMessageWriter(log)
                .resolveFromJson(descrArtifact.getFile().toPath());
    }

    private Artifact resolveJsonOrNull(MavenArtifactResolver mvn, String bomGroupId, String bomArtifactId, String bomVersion) {
        Artifact jsonArtifact = new DefaultArtifact(bomGroupId, bomArtifactId, null, "json", bomVersion);
        try {
            jsonArtifact = mvn.resolve(jsonArtifact).getArtifact();
        } catch (Exception e) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Failed to resolve JSON descriptor as " + jsonArtifact);
            }
            jsonArtifact = new DefaultArtifact(bomGroupId, bomArtifactId + "-descriptor-json", null, "json",
                    bomVersion);
            try {
                jsonArtifact = mvn.resolve(jsonArtifact).getArtifact();
            } catch (Exception e1) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Failed to resolve JSON descriptor as " + jsonArtifact);
                }
                return null;
            }
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Resolve JSON descriptor " + jsonArtifact);
        }
        return jsonArtifact;
    }

    protected void validateParameters() throws MojoExecutionException {
    }

    protected abstract void doExecute(QuarkusProject quarkusProject, MessageWriter log)
            throws MojoExecutionException;

    private String resolveValue(String expr) throws IOException {
        if (expr.startsWith("${") && expr.endsWith("}")) {
            final String name = expr.substring(2, expr.length() - 1);
            final String v = project.getModel().getProperties().getProperty(name);
            if (v == null) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Failed to resolve property " + name);
                }
            }
            return v;
        }
        return expr;
    }

    private List<Dependency> getManagedDependencies() throws IOException {
        final DependencyManagement managed = project.getModel().getDependencyManagement();
        return managed != null ? managed.getDependencies()
                : Collections.emptyList();
    }
}
