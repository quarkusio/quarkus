package io.quarkus.maven;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.platform.descriptor.CombinedQuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
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

    @Component
    RemoteRepositoryManager remoteRepositoryManager;

    private Artifact projectArtifact;
    private ArtifactDescriptorResult projectDescr;
    private MavenArtifactResolver artifactResolver;

    @Override
    public void execute() throws MojoExecutionException {

        // Validate Mojo parameters
        validateParameters();

        final MessageWriter log = new MojoMessageWriter(getLog());
        final Path projectDirPath = baseDir();
        BuildTool buildTool = QuarkusProject.resolveExistingProjectBuildTool(projectDirPath);
        if (buildTool == null) {
            // it's not Gradle and the pom.xml not found, so we assume there is not project at all
            buildTool = BuildTool.MAVEN;
        }

        final QuarkusPlatformDescriptor platformDescriptor = resolvePlatformDescriptor(log);
        final QuarkusProject quarkusProject;
        if (BuildTool.MAVEN.equals(buildTool) && project.getFile() != null) {
            quarkusProject = QuarkusProject.of(baseDir(), platformDescriptor,
                    new MavenProjectBuildFile(baseDir(), platformDescriptor, () -> project.getOriginalModel(),
                            () -> {
                                try {
                                    return projectDependencies();
                                } catch (MojoExecutionException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> {
                                try {
                                    return projectDescriptor().getManagedDependencies();
                                } catch (MojoExecutionException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            project.getModel().getProperties()));
        } else {
            quarkusProject = QuarkusProject.of(baseDir(), platformDescriptor, buildTool);
        }

        doExecute(quarkusProject, log);
    }

    private ArtifactDescriptorResult projectDescriptor() throws MojoExecutionException {
        if (this.projectDescr == null) {
            try {
                projectDescr = artifactResolver.resolveDescriptor(projectArtifact());
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to read the artifact desriptor for the project", e);
            }
        }
        return projectDescr;
    }

    protected Path baseDir() {
        return project == null || project.getBasedir() == null ? Paths.get("").normalize().toAbsolutePath()
                : project.getBasedir().toPath();
    }

    private QuarkusPlatformDescriptor resolvePlatformDescriptor(final MessageWriter log) throws MojoExecutionException {
        // Resolve and setup the platform descriptor
        try {
            final MavenArtifactResolver mvn = artifactResolver();
            if (project.getFile() != null) {
                List<Artifact> descrArtifactList = collectQuarkusPlatformDescriptors(log, mvn);
                if (descrArtifactList.isEmpty()) {
                    descrArtifactList = resolveLegacyQuarkusPlatformDescriptors(log, mvn);
                }
                if (!descrArtifactList.isEmpty()) {
                    final QuarkusJsonPlatformDescriptorResolver descriptorResolver = QuarkusJsonPlatformDescriptorResolver
                            .newInstance()
                            .setArtifactResolver(new BootstrapAppModelResolver(mvn))
                            .setMessageWriter(log);

                    if (descrArtifactList.size() == 1) {
                        return descriptorResolver.resolveFromJson(descrArtifactList.get(0).getFile().toPath());
                    }

                    // Typically, quarkus-bom platform will appear first.
                    // The descriptors that are generated today are not fragmented and include everything
                    // a platform offers. Which means if the quarkus-bom platform appears first and its version
                    // matches the Quarkus core version of the platform built on top of the quarkus-bom
                    // (e.g. quarkus-universe-bom) the quarkus-bom platform can be skipped,
                    // since it will already be included in the platform that's built on top of it
                    int i = 0;
                    Artifact platformArtifact = descrArtifactList.get(0);
                    final String quarkusBomPlatformArtifactId = "quarkus-bom"
                            + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX;
                    Artifact quarkusBomPlatformArtifact = null;
                    if (quarkusBomPlatformArtifactId.equals(platformArtifact.getArtifactId())) {
                        quarkusBomPlatformArtifact = platformArtifact;
                    }
                    final CombinedQuarkusPlatformDescriptor.Builder builder = CombinedQuarkusPlatformDescriptor.builder();
                    while (++i < descrArtifactList.size()) {
                        platformArtifact = descrArtifactList.get(i);
                        final QuarkusPlatformDescriptor descriptor = descriptorResolver
                                .resolveFromJson(platformArtifact.getFile().toPath());
                        if (quarkusBomPlatformArtifact != null) {
                            if (!quarkusBomPlatformArtifact.getVersion().equals(descriptor.getQuarkusVersion())) {
                                builder.addPlatform(
                                        descriptorResolver.resolveFromJson(quarkusBomPlatformArtifact.getFile().toPath()));
                            }
                            quarkusBomPlatformArtifact = null;
                        }
                        builder.addPlatform(descriptorResolver.resolveFromJson(platformArtifact.getFile().toPath()));
                    }
                    return builder.build();
                }
            }
            return CreateUtils.resolvePlatformDescriptor(bomGroupId, bomArtifactId, bomVersion, mvn, getLog());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize maven artifact resolver", e);
        }
    }

    private MavenArtifactResolver artifactResolver() throws BootstrapMavenException {
        if (artifactResolver == null) {
            artifactResolver = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .setRemoteRepositoryManager(remoteRepositoryManager)
                    .build();
        }
        return artifactResolver;
    }

    private List<Artifact> resolveLegacyQuarkusPlatformDescriptors(MessageWriter log, MavenArtifactResolver mvn)
            throws IOException {
        final List<Artifact> descrArtifactList = new ArrayList<>(2);
        for (Dependency dep : getManagedDependencies(mvn)) {
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
                log.debug("Found legacy platform %s", jsonArtifact);
                descrArtifactList.add(jsonArtifact);
            }
        }
        return descrArtifactList;
    }

    private List<Artifact> collectQuarkusPlatformDescriptors(MessageWriter log, MavenArtifactResolver mvn)
            throws MojoExecutionException {
        final List<Artifact> descrArtifactList = new ArrayList<>(2);
        final List<Dependency> constraints = project.getDependencyManagement() == null ? Collections.emptyList()
                : project.getDependencyManagement().getDependencies();
        if (!constraints.isEmpty()) {
            for (Dependency d : constraints) {
                if (!("json".equals(d.getType())
                        && d.getArtifactId().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX))) {
                    continue;
                }
                final Artifact a = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getClassifier(),
                        d.getType(), d.getVersion());
                try {
                    log.debug("Found platform descriptor %s", a);
                    descrArtifactList.add(mvn.resolve(a).getArtifact());
                } catch (Exception e) {
                    throw new MojoExecutionException("Failed to resolve the platform descriptor " + a, e);
                }
            }
        }
        return descrArtifactList;
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

    private List<Dependency> getManagedDependencies(MavenArtifactResolver resolver) throws IOException {
        List<Dependency> managedDependencies = new ArrayList<>();
        Model model = project.getOriginalModel();
        DependencyManagement managed = model.getDependencyManagement();
        if (managed != null) {
            managedDependencies.addAll(managed.getDependencies());
        }
        Parent parent;
        while ((parent = model.getParent()) != null) {
            try {
                ArtifactResult result = resolver.resolve(new DefaultArtifact(
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        "pom",
                        ModelUtils.resolveVersion(parent.getVersion(), model)));
                model = ModelUtils.readModel(result.getArtifact().getFile().toPath());
                managed = model.getDependencyManagement();
                if (managed != null) {
                    // Alexey Loubyansky: In Maven whatever is imported first has a priority
                    // So to match the maven way, we should be reading the root parent first
                    managedDependencies.addAll(0, managed.getDependencies());
                }
            } catch (BootstrapMavenException e) {
                // ignore
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Error while resolving descriptor", e);
                }
                break;
            }
        }
        return managedDependencies;
    }

    private List<org.eclipse.aether.graph.Dependency> projectDependencies() throws MojoExecutionException {
        final List<org.eclipse.aether.graph.Dependency> deps = new ArrayList<>();
        try {
            artifactResolver().collectDependencies(projectArtifact(), Collections.emptyList())
                    .getRoot().accept(new DependencyVisitor() {
                        @Override
                        public boolean visitEnter(DependencyNode node) {
                            if (node.getDependency() != null) {
                                deps.add(node.getDependency());
                            }
                            return true;
                        }

                        @Override
                        public boolean visitLeave(DependencyNode node) {
                            return true;
                        }
                    });
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to collect dependencies for the project", e);
        }
        return deps;
    }

    private Artifact projectArtifact() {
        return projectArtifact == null
                ? projectArtifact = new DefaultArtifact(project.getGroupId(), project.getArtifactId(), null, "pom",
                        project.getVersion())
                : projectArtifact;
    }
}
