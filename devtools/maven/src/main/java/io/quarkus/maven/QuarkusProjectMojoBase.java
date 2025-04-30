package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.CodestartResourceLoadersBuilder;
import io.quarkus.devtools.project.JavaVersion;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.buildfile.MavenProjectBuildFile;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.platform.tools.maven.MojoMessageWriter;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformStream;

public abstract class QuarkusProjectMojoBase extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    MavenSession session;

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
    QuarkusWorkspaceProvider workspaceProvider;

    private List<ArtifactCoords> importedPlatforms;

    private Artifact projectArtifact;
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
            // it's not Gradle and the pom.xml not found, so we assume there is no project at all
            buildTool = BuildTool.MAVEN;
        }

        final QuarkusProject quarkusProject;
        if (BuildTool.MAVEN.equals(buildTool) && project.getFile() != null) {
            try {
                quarkusProject = MavenProjectBuildFile.getProject(projectArtifact(), project.getOriginalModel(), baseDir(),
                        project.getModel().getProperties(), artifactResolver(), getExtensionCatalogResolver(),
                        getMessageWriter(),
                        null);
            } catch (RegistryResolutionException e) {
                throw new MojoExecutionException("Failed to initialize Quarkus Maven extension manager", e);
            }
        } else {
            final ExtensionCatalog extensionCatalog = resolveExtensionCatalog();
            final List<ResourceLoader> codestartsResourceLoader = CodestartResourceLoadersBuilder
                    .codestartLoadersBuilder()
                    .artifactResolver(artifactResolver())
                    .catalog(extensionCatalog)
                    .build();
            quarkusProject = QuarkusProject.of(baseDir(), extensionCatalog,
                    codestartsResourceLoader, log, buildTool, JavaVersion.NA);
        }

        doExecute(quarkusProject, getMessageWriter());
    }

    protected MessageWriter getMessageWriter() {
        return log == null ? log = new MojoMessageWriter(getLog()) : log;
    }

    protected Path baseDir() {
        return project == null || project.getBasedir() == null ? Paths.get("").normalize().toAbsolutePath()
                : project.getBasedir().toPath();
    }

    private ExtensionCatalog resolveExtensionCatalog() throws MojoExecutionException {
        final ExtensionCatalogResolver catalogResolver = getExtensionCatalogResolver();
        if (catalogResolver.hasRegistries()) {
            try {
                return catalogResolver.resolveExtensionCatalog(getImportedPlatforms());
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to resolve the Quarkus extension catalog", e);
            }
        }
        return ToolsUtils.mergePlatforms(collectImportedPlatforms(), catalogArtifactResolver());
    }

    protected ExtensionCatalogResolver getExtensionCatalogResolver() throws MojoExecutionException {
        if (catalogResolver == null) {
            try {
                catalogResolver = QuarkusProjectHelper.isRegistryClientEnabled()
                        ? QuarkusProjectHelper.getCatalogResolver(catalogArtifactResolver(), getMessageWriter())
                        : ExtensionCatalogResolver.empty();
            } catch (RegistryResolutionException e) {
                throw new MojoExecutionException("Failed to initialize Quarkus extension resolver", e);
            }
        }
        return catalogResolver;
    }

    protected List<ArtifactCoords> getImportedPlatforms() throws MojoExecutionException {
        if (importedPlatforms == null) {
            if (project.getFile() == null) {
                if (bomGroupId == null && bomArtifactId == null && bomVersion == null) {
                    return List.of();
                }
                final ExtensionCatalogResolver catalogResolver = getExtensionCatalogResolver();
                if (!catalogResolver.hasRegistries()) {
                    throw new MojoExecutionException(
                            "Couldn't resolve the Quarkus platform catalog since none of the Quarkus extension registries are available");
                }
                if (bomGroupId == null) {
                    bomGroupId = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID;
                }
                ArtifactCoords platformBom = null;
                try {
                    platformBom = getSingleMatchingBom(bomGroupId, bomArtifactId, bomVersion,
                            getExtensionCatalogResolver().resolvePlatformCatalog());
                } catch (RegistryResolutionException e) {
                    throw new MojoExecutionException("Failed to resolve the catalog of Quarkus platforms", e);
                }
                return importedPlatforms = List.of(platformBom);
            }
            importedPlatforms = collectImportedPlatforms();
        }
        return importedPlatforms;
    }

    protected MavenArtifactResolver catalogArtifactResolver() throws MojoExecutionException {
        return artifactResolver();
    }

    protected MavenArtifactResolver artifactResolver() throws MojoExecutionException {
        return artifactResolver == null ? artifactResolver = initArtifactResolver() : artifactResolver;
    }

    protected MavenArtifactResolver initArtifactResolver() throws MojoExecutionException {
        var config = BootstrapMavenContext.config()
                .setArtifactTransferLogging(getLog().isDebugEnabled())
                .setRemoteRepositoryManager(workspaceProvider.getRemoteRepositoryManager())
                .setRepositorySystem(workspaceProvider.getRepositorySystem())
                .setRemoteRepositories(repos)
                .setWorkspaceDiscovery(false)
                .setRepositorySystemSession(repoSession);
        return workspaceProvider.createArtifactResolver(config);
    }

    private List<ArtifactCoords> collectImportedPlatforms() {
        final List<ArtifactCoords> descriptors = new ArrayList<>(4);
        final List<Dependency> constraints = project.getDependencyManagement() == null ? List.of()
                : project.getDependencyManagement().getDependencies();
        if (!constraints.isEmpty()) {
            final MessageWriter log = getMessageWriter();
            for (Dependency d : constraints) {
                if (!("json".equals(d.getType())
                        && d.getArtifactId().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX))) {
                    continue;
                }
                final ArtifactCoords a = ArtifactCoords.of(d.getGroupId(), d.getArtifactId(), d.getClassifier(),
                        d.getType(), d.getVersion());
                descriptors.add(a);
                log.debug("Found platform descriptor %s", a);
            }
        }
        return descriptors;
    }

    protected void validateParameters() throws MojoExecutionException {
    }

    protected abstract void doExecute(QuarkusProject quarkusProject, MessageWriter log)
            throws MojoExecutionException;

    private Artifact projectArtifact() {
        return projectArtifact == null
                ? projectArtifact = new DefaultArtifact(project.getGroupId(), project.getArtifactId(), null,
                        ArtifactCoords.TYPE_POM, project.getVersion())
                : projectArtifact;
    }

    static ArtifactCoords getSingleMatchingBom(String bomGroupId, String bomArtifactId, String bomVersion,
            PlatformCatalog platformCatalog) throws MojoExecutionException {
        if (bomGroupId == null && bomArtifactId == null && bomVersion == null) {
            return null;
        }
        if (bomGroupId == null) {
            bomGroupId = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID;
        }
        ArtifactCoords platformBom = null;
        List<ArtifactCoords> matches = null;
        for (Platform p : platformCatalog.getPlatforms()) {
            if (!p.getPlatformKey().equals(bomGroupId)) {
                continue;
            }
            for (PlatformStream s : p.getStreams()) {
                for (PlatformRelease r : s.getReleases()) {
                    for (ArtifactCoords bom : r.getMemberBoms()) {
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
                }
            }
        }
        if (matches != null) {
            StringWriter buf = new StringWriter();
            buf.append("Multiple platforms were matching the requested platform BOM coordinates ");
            buf.append(bomGroupId == null ? "*" : bomGroupId).append(':');
            buf.append(bomArtifactId == null ? "*" : bomArtifactId).append(':');
            buf.append(bomVersion == null ? "*" : bomVersion).append(": ");
            try (BufferedWriter writer = new BufferedWriter(buf)) {
                for (ArtifactCoords bom : matches) {
                    writer.newLine();
                    writer.append("- ").append(bom.toString());
                }
            } catch (IOException e) {
                //
            }
            throw new MojoExecutionException(buf.toString());
        }
        return platformBom;
    }
}
