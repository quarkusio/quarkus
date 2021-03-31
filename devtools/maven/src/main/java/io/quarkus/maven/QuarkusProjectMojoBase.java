package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.platform.tools.maven.MojoMessageWriter;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;

public abstract class QuarkusProjectMojoBase extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Component
    protected RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    protected RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    protected List<RemoteRepository> repos;

    @Parameter(property = "bomGroupId", required = false)
    private String bomGroupId;

    @Parameter(property = "bomArtifactId", required = false)
    private String bomArtifactId;

    @Parameter(property = "bomVersion", required = false)
    private String bomVersion;

    @Component
    RemoteRepositoryManager remoteRepositoryManager;

    @Parameter(property = "enableRegistryClient")
    private boolean enableRegistryClient;

    private List<ArtifactCoords> importedPlatforms;

    private Artifact projectArtifact;
    private ArtifactDescriptorResult projectDescr;
    private MavenArtifactResolver artifactResolver;
    private ExtensionCatalogResolver catalogResolver;
    private MessageWriter log;

    @Override
    public void execute() throws MojoExecutionException {

        // Validate Mojo parameters
        validateParameters();

        final Path projectDirPath = baseDir();
        BuildTool buildTool = QuarkusProject.resolveExistingProjectBuildTool(projectDirPath);
        if (buildTool == null) {
            // it's not Gradle and the pom.xml not found, so we assume there is not project at all
            buildTool = BuildTool.MAVEN;
        }

        final ExtensionCatalog catalog = resolveExtensionsCatalog();
        final List<ResourceLoader> codestartsResourceLoaders = QuarkusProjectHelper.getCodestartResourceLoaders(catalog,
                artifactResolver());
        final QuarkusProject quarkusProject;
        if (BuildTool.MAVEN.equals(buildTool) && project.getFile() != null) {
            quarkusProject = QuarkusProject.of(baseDir(), catalog, codestartsResourceLoaders, getMessageWriter(),
                    new MavenProjectBuildFile(baseDir(), catalog, () -> project.getOriginalModel(),
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
            quarkusProject = QuarkusProject.of(baseDir(), catalog, codestartsResourceLoaders, log, buildTool);
        }

        doExecute(quarkusProject, getMessageWriter());
    }

    protected MessageWriter getMessageWriter() {
        return log == null ? log = new MojoMessageWriter(getLog()) : log;
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

    protected boolean isLimitExtensionsToImportedPlatforms() {
        return false;
    }

    private ExtensionCatalog resolveExtensionsCatalog() throws MojoExecutionException {
        final ExtensionCatalogResolver catalogResolver = enableRegistryClient ? getExtensionCatalogResolver()
                : ExtensionCatalogResolver.empty();
        if (catalogResolver.hasRegistries()) {
            try {
                return isLimitExtensionsToImportedPlatforms()
                        ? catalogResolver.resolveExtensionCatalog(getImportedPlatforms())
                        : catalogResolver.resolveExtensionCatalog(getQuarkusCoreVersion());
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to resolve the Quarkus extensions catalog", e);
            }
        }
        return ToolsUtils.mergePlatforms(collectImportedPlatforms(), artifactResolver());
    }

    protected ExtensionCatalogResolver getExtensionCatalogResolver() throws MojoExecutionException {
        return catalogResolver == null
                ? catalogResolver = QuarkusProjectHelper.getCatalogResolver(artifactResolver(), getMessageWriter())
                : catalogResolver;
    }

    protected List<ArtifactCoords> getImportedPlatforms() throws MojoExecutionException {
        if (importedPlatforms == null) {
            if (project.getFile() == null) {
                if (bomGroupId == null && bomArtifactId == null && bomVersion == null) {
                    return Collections.emptyList();
                }
                if (bomGroupId == null) {
                    bomGroupId = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID;
                }
                final ExtensionCatalogResolver catalogResolver = getExtensionCatalogResolver();
                ArtifactCoords platformBom = null;
                List<ArtifactCoords> matches = null;
                try {
                    for (Platform p : catalogResolver.resolvePlatformCatalog().getPlatforms()) {
                        final ArtifactCoords bom = p.getBom();
                        if (bomGroupId != null && !bom.getGroupId().equals(bomGroupId)) {
                            continue;
                        }
                        if (bomArtifactId != null && !bom.getArtifactId().equals(bomArtifactId)) {
                            continue;
                        }
                        if (bomVersion != null && !bom.getVersion().equals(bomVersion)) {
                            continue;
                        }
                        if (platformBom == null) {
                            platformBom = bom;
                        } else {
                            if (matches == null) {
                                matches = new ArrayList<>();
                                matches.add(platformBom);
                            }
                            matches.add(bom);
                        }
                    }
                } catch (RegistryResolutionException e) {
                    throw new MojoExecutionException("Failed to resolve the catalog of Quarkus platforms", e);
                }
                if (matches != null) {
                    final StringWriter buf = new StringWriter();
                    buf.append("Found multiple platforms matching the provided arguments: ");
                    try (BufferedWriter writer = new BufferedWriter(buf)) {
                        for (ArtifactCoords coords : matches) {
                            writer.newLine();
                            writer.append("- ").append(coords.toString());
                        }
                    } catch (IOException e) {
                        buf.append(matches.toString());
                    }
                    throw new MojoExecutionException(buf.toString());
                }
                return importedPlatforms = Collections.singletonList(platformBom);
            }
            importedPlatforms = collectImportedPlatforms();
        }
        return importedPlatforms;
    }

    private MavenArtifactResolver artifactResolver() throws MojoExecutionException {
        if (artifactResolver == null) {
            try {
                artifactResolver = MavenArtifactResolver.builder()
                        .setRepositorySystem(repoSystem)
                        .setRepositorySystemSession(repoSession)
                        .setRemoteRepositories(repos)
                        .setRemoteRepositoryManager(remoteRepositoryManager)
                        .build();
            } catch (BootstrapMavenException e) {
                throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
            }
        }
        return artifactResolver;
    }

    private List<ArtifactCoords> collectImportedPlatforms()
            throws MojoExecutionException {
        final List<ArtifactCoords> descriptors = new ArrayList<>(4);
        final List<Dependency> constraints = project.getDependencyManagement() == null ? Collections.emptyList()
                : project.getDependencyManagement().getDependencies();
        if (!constraints.isEmpty()) {
            final MessageWriter log = getMessageWriter();
            for (Dependency d : constraints) {
                if (!("json".equals(d.getType())
                        && d.getArtifactId().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX))) {
                    continue;
                }
                final ArtifactCoords a = new ArtifactCoords(d.getGroupId(), d.getArtifactId(), d.getClassifier(),
                        d.getType(), d.getVersion());
                descriptors.add(a);
                log.debug("Found platform descriptor %s", a);
            }
        }
        return descriptors;
    }

    private String getQuarkusCoreVersion() {
        final List<Dependency> constraints = project.getDependencyManagement() == null ? Collections.emptyList()
                : project.getDependencyManagement().getDependencies();
        for (Dependency d : constraints) {
            if (d.getArtifactId().endsWith("quarkus-core") && d.getGroupId().equals("io.quarkus")) {
                return d.getVersion();
            }
        }
        return null;
    }

    protected void validateParameters() throws MojoExecutionException {
    }

    protected abstract void doExecute(QuarkusProject quarkusProject, MessageWriter log)
            throws MojoExecutionException;

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
