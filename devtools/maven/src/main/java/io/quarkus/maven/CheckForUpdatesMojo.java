package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.platform.tools.maven.MojoMessageWriter;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.selection.ExtensionOrigins;
import io.quarkus.registry.catalog.selection.OriginCombination;
import io.quarkus.registry.catalog.selection.OriginPreference;
import io.quarkus.registry.catalog.selection.OriginSelector;
import io.quarkus.registry.util.PlatformArtifacts;

/**
 * NOTE: this mojo is experimental
 */
@Mojo(name = "check-for-updates", requiresProject = true)
public class CheckForUpdatesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Component
    private RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepoManager;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().warn(
                "This goal is experimental. Its name, parameters, output and implementation will be evolving without the promise of keeping backward compatibility");

        if (project.getFile() == null) {
            throw new MojoExecutionException("This goal requires a project");
        }

        if (!QuarkusProjectHelper.isRegistryClientEnabled()) {
            throw new MojoExecutionException("This goal requires a Quarkus extension registry client to be enabled");
        }

        final MavenArtifactResolver mvn;
        try {
            mvn = MavenArtifactResolver.builder()
                    .setArtifactTransferLogging(getLog().isDebugEnabled())
                    .setRemoteRepositories(repos)
                    .setRemoteRepositoryManager(remoteRepoManager)
                    .setPreferPomsFromWorkspace(true)
                    .build();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
        }
        final Collection<Path> createdDirs = ensureResolvable(mvn, new DefaultArtifact(project.getGroupId(),
                project.getArtifactId(), ArtifactCoords.TYPE_POM, project.getVersion()));
        try {
            checkForUpdates(mvn);
        } finally {
            for (Path p : createdDirs) {
                IoUtils.recursiveDelete(p);
            }
        }
    }

    private void checkForUpdates(MavenArtifactResolver mvn) throws MojoExecutionException {

        final Map<ArtifactKey, String> previousExtensions = getDirectExtensionDependencies(mvn);
        if (previousExtensions.isEmpty()) {
            getLog().info("The project does not appear to depend on any Quarkus extension directly");
            return;
        }

        final MojoMessageWriter log = new MojoMessageWriter(getLog());
        final ExtensionCatalogResolver catalogResolver;
        try {
            catalogResolver = QuarkusProjectHelper.getCatalogResolver(mvn, log);
        } catch (RegistryResolutionException e) {
            throw new MojoExecutionException("Failed to initialize Quarkus extension registry client", e);
        }

        if (!catalogResolver.hasRegistries()) {
            throw new MojoExecutionException("Configured Quarkus extension registries aren't available");
        }

        final ExtensionCatalog latestCatalog;
        try {
            latestCatalog = catalogResolver.resolveExtensionCatalog();
        } catch (RegistryResolutionException e) {
            throw new MojoExecutionException(
                    "Failed to resolve the latest Quarkus extension catalog from the configured extension registries", e);
        }

        final Map<ArtifactKey, Extension> recommendedExtensionMap = new HashMap<>(latestCatalog.getExtensions().size());
        final Map<String, List<ArtifactKey>> recommendedExtensionsByOrigin = new HashMap<>();
        for (Extension e : latestCatalog.getExtensions()) {
            recommendedExtensionMap.put(e.getArtifact().getKey(), e);
            for (ExtensionOrigin origin : e.getOrigins()) {
                recommendedExtensionsByOrigin.computeIfAbsent(origin.getId(), k -> new ArrayList<>())
                        .add(e.getArtifact().getKey());
            }
        }

        final List<ArtifactKey> notAvailableExtensions = new ArrayList<>(0);
        final List<Extension> recommendedExtensions = new ArrayList<>(previousExtensions.size());
        for (ArtifactKey key : previousExtensions.keySet()) {
            final Extension e = recommendedExtensionMap.get(key);
            if (e == null) {
                notAvailableExtensions.add(key);
            } else {
                recommendedExtensions.add(e);
            }
        }

        if (!notAvailableExtensions.isEmpty()) {
            final StringWriter buf = new StringWriter();
            try (BufferedWriter writer = new BufferedWriter(buf)) {
                writer.append(
                        "Could not find any information about the following extensions in the currently configured registries: ");
                writer.newLine();
                writer.append(" - ").append(notAvailableExtensions.get(0).toGacString());
                for (int i = 1; i < notAvailableExtensions.size(); ++i) {
                    writer.newLine();
                    writer.append(" - ").append(notAvailableExtensions.get(i).toGacString());
                }
            } catch (IOException e) {
                getLog().warn("Failed to compose a warning message", e);
            }
            getLog().warn(buf.toString());
            return;
        }

        final List<ExtensionCatalog> recommendedOrigins;
        try {
            recommendedOrigins = getRecommendedOrigins(latestCatalog, recommendedExtensions);
        } catch (QuarkusCommandException e) {
            getLog().warn(e.getLocalizedMessage());
            return;
        }

        ArtifactCoords rhUniverseBom = null;
        ArtifactCoords rhProdBom = null;
        final List<ArtifactCoords> previousBomImports = new ArrayList<>();
        for (Dependency d : project.getDependencyManagement().getDependencies()) {
            if (!PlatformArtifacts.isCatalogArtifactId(d.getArtifactId())) {
                continue;
            }
            final ArtifactCoords platformBomCoords = new ArtifactCoords(d.getGroupId(),
                    PlatformArtifacts.ensureBomArtifactId(d.getArtifactId()), "pom", d.getVersion());
            if (d.getArtifactId().startsWith("quarkus-universe-bom-")) {
                // in pre-2.x quarkus versions, the quarkus-bom descriptor would show up as a
                // parent of the quarkus-universe-bom one
                // even if it was not actually imported, so here we simply remove it, if it was
                // found
                previousBomImports.remove(new ArtifactCoords(platformBomCoords.getGroupId(), "quarkus-bom", "pom",
                        platformBomCoords.getVersion()));

                if (platformBomCoords.getVersion().contains("-redhat-")) {
                    rhUniverseBom = platformBomCoords;
                } else if (rhUniverseBom != null && platformBomCoords.getVersion().regionMatches(0, rhUniverseBom.getVersion(),
                        0, rhUniverseBom.getVersion().length() - 1 - rhUniverseBom.getVersion().indexOf("-redhat-"))) {
                    continue;
                }
            } else if (d.getArtifactId().startsWith("quarkus-product-bom-")) {
                // rhbq 1.x filtering
                rhProdBom = platformBomCoords;
                if (previousBomImports.contains(new ArtifactCoords(platformBomCoords.getGroupId(),
                        "quarkus-universe-bom", "pom", platformBomCoords.getVersion()))) {
                    continue;
                }
            } else if (rhProdBom != null && platformBomCoords.getVersion().equals(rhProdBom.getVersion()) // rhbq 1.x filtering
                    && platformBomCoords.getArtifactId().equals("quarkus-bom")) {
                continue;
            }
            previousBomImports.add(platformBomCoords);
        }

        final List<ArtifactCoords> recommendedBomImports = new ArrayList<>();
        final Map<ArtifactCoords, String> nonPlatformUpdates = new LinkedHashMap<>(0);

        for (ExtensionCatalog origin : recommendedOrigins) {
            if (origin.isPlatform()) {
                if (!previousBomImports.remove(origin.getBom())) {
                    recommendedBomImports.add(origin.getBom());
                }
                for (ArtifactKey extKey : recommendedExtensionsByOrigin.getOrDefault(origin.getId(), Collections.emptyList())) {
                    previousExtensions.remove(extKey);
                }
            } else {
                for (ArtifactKey extKey : recommendedExtensionsByOrigin.getOrDefault(origin.getId(), Collections.emptyList())) {
                    final Extension recommendedExt = recommendedExtensionMap.get(extKey);
                    final String prevVersion = previousExtensions.remove(extKey);
                    if (prevVersion != null && !prevVersion.equals(recommendedExt.getArtifact().getVersion())) {
                        nonPlatformUpdates.put(recommendedExt.getArtifact(), prevVersion);
                    }
                }
            }
        }

        if (recommendedBomImports.isEmpty() && nonPlatformUpdates.isEmpty()) {
            log.info("The project is up-to-date");
            return;
        }

        ArtifactCoords prevPluginCoords = null;
        for (Plugin p : project.getBuildPlugins()) {
            if (p.getArtifactId().equals("quarkus-maven-plugin")) {
                prevPluginCoords = new ArtifactCoords(p.getGroupId(), p.getArtifactId(), p.getVersion());
                break;
            }
        }

        ExtensionCatalog core = null;
        for (ExtensionOrigin o : recommendedOrigins) {
            if (o.isPlatform() && o.getBom().getArtifactId().equals("quarkus-bom")) {
                core = (ExtensionCatalog) o;
            }
        }

        ArtifactCoords recommendedPluginCoords = null;
        if (core != null) {
            @SuppressWarnings("unchecked")
            final Map<String, ?> props = (Map<String, ?>) ((Map<String, Object>) core.getMetadata().getOrDefault("project",
                    Collections.emptyMap())).getOrDefault("properties", Collections.emptyMap());
            final String pluginGroupId = (String) props.get("maven-plugin-groupId");
            final String pluginArtifactId = (String) props.get("maven-plugin-artifactId");
            final String pluginVersion = (String) props.get("maven-plugin-version");
            if (pluginGroupId == null || pluginArtifactId == null || pluginVersion == null) {
                log.warn("Failed to locate the recommended Quarkus Maven plugin coordinates");
            } else {
                recommendedPluginCoords = new ArtifactCoords(pluginGroupId, pluginArtifactId, pluginVersion);
            }
        }

        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {

            writer.append("Currently recommended updates for the application include:");
            writer.newLine();
            writer.newLine();

            if (!previousBomImports.isEmpty()) {
                writer.append(" * BOM imports to be replaced:");
                writer.newLine();
                writer.newLine();
                for (ArtifactCoords bom : previousBomImports) {
                    logBomImport(writer, bom);
                }
                writer.newLine();
            }

            if (!recommendedBomImports.isEmpty()) {
                writer.append(" * New recommended BOM imports:");
                writer.newLine();
                writer.newLine();
                for (ArtifactCoords bom : recommendedBomImports) {
                    logBomImport(writer, bom);
                }
                writer.newLine();
            }

            if (!nonPlatformUpdates.isEmpty()) {
                writer.append(" * New recommended extension versions (not managed by the BOMs):");
                writer.newLine();
                writer.newLine();

                for (ArtifactCoords coords : nonPlatformUpdates.keySet()) {
                    writer.append("    <dependency>");
                    writer.newLine();
                    writer.append("      <groupId>").append(coords.getGroupId()).append("</groupId>");
                    writer.newLine();
                    writer.append("      <artifactId>").append(coords.getArtifactId()).append("</artifactId>");
                    writer.newLine();
                    writer.append("      <version>").append(coords.getVersion()).append("</version>");
                    writer.newLine();
                    writer.append("    </dependency>");
                    writer.newLine();
                }
                writer.newLine();
            }

            if (prevPluginCoords != null && recommendedPluginCoords != null
                    && !prevPluginCoords.equals(recommendedPluginCoords)) {
                writer.append(" * Recommended Quarkus Maven plugin:");
                writer.newLine();
                writer.newLine();
                writer.append("      <plugin>");
                writer.newLine();
                writer.append("        <groupId>").append(recommendedPluginCoords.getGroupId()).append("</groupId>");
                writer.newLine();
                writer.append("        <artifactId>").append(recommendedPluginCoords.getArtifactId()).append("</artifactId>");
                writer.newLine();
                writer.append("        <version>").append(recommendedPluginCoords.getVersion()).append("</version>");
                writer.newLine();
                writer.append("      </plugin>");
                writer.newLine();

            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to compose the update report", e);
        }

        getLog().info(buf.toString());
    }

    private void logBomImport(BufferedWriter writer, ArtifactCoords bom) throws IOException {
        writer.append("      <dependency>");
        writer.newLine();
        writer.append("        <groupId>").append(bom.getGroupId()).append("</groupId>");
        writer.newLine();
        writer.append("        <artifactId>").append(bom.getArtifactId()).append("</artifactId>");
        writer.newLine();
        writer.append("        <version>").append(bom.getVersion()).append("</version>");
        writer.newLine();
        writer.append("        <type>pom</type>");
        writer.newLine();
        writer.append("        <scope>import</scope>");
        writer.newLine();
        writer.append("      </dependency>");
        writer.newLine();
    }

    private Map<ArtifactKey, String> getDirectExtensionDependencies(MavenArtifactResolver resolver)
            throws MojoExecutionException {
        final List<Dependency> modelDeps = project.getModel().getDependencies();
        final Map<ArtifactKey, String> extensions = new HashMap<>(modelDeps.size());
        for (Dependency d : modelDeps) {
            if (!ArtifactCoords.TYPE_JAR.equals(d.getType())) {
                continue;
            }
            final ArtifactResult ar;
            try {
                ar = resolver.resolve(
                        new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion()));
            } catch (BootstrapMavenException e) {
                throw new MojoExecutionException("Failed to resolve project dependencies", e);
            }
            final Artifact a = ar.getArtifact();
            if (isExtension(a.getFile().toPath())) {
                extensions.put(new ArtifactKey(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getExtension()),
                        a.getVersion());
            }
        }
        return extensions;
    }

    private static boolean isExtension(Path p) throws MojoExecutionException {
        if (!Files.exists(p)) {
            throw new MojoExecutionException("Extension artifact " + p + " does not exist");
        }
        if (Files.isDirectory(p)) {
            return Files.exists(p.resolve(BootstrapConstants.DESCRIPTOR_PATH));
        } else {
            try (FileSystem fs = ZipUtils.newFileSystem(p)) {
                return Files.exists(fs.getPath(BootstrapConstants.DESCRIPTOR_PATH));
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read archive " + p, e);
            }
        }
    }

    private static List<ExtensionCatalog> getRecommendedOrigins(ExtensionCatalog extensionCatalog, List<Extension> extensions)
            throws QuarkusCommandException {
        final List<ExtensionOrigins> extOrigins = new ArrayList<>(extensions.size());
        for (Extension e : extensions) {
            addOrigins(extOrigins, e);
        }

        final OriginCombination recommendedCombination = OriginSelector.of(extOrigins).calculateRecommendedCombination();
        if (recommendedCombination == null) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Failed to determine a compatible Quarkus version for the requested extensions: ");
            buf.append(extensions.get(0).getArtifact().getKey().toGacString());
            for (int i = 1; i < extensions.size(); ++i) {
                buf.append(", ").append(extensions.get(i).getArtifact().getKey().toGacString());
            }
            throw new QuarkusCommandException(buf.toString());
        }
        return recommendedCombination.getUniqueSortedOrigins().stream().map(o -> o.getCatalog()).collect(Collectors.toList());
    }

    private static void addOrigins(final List<ExtensionOrigins> extOrigins, Extension e) {
        ExtensionOrigins.Builder eoBuilder = null;
        for (ExtensionOrigin o : e.getOrigins()) {
            if (!(o instanceof ExtensionCatalog)) {
                continue;
            }
            final ExtensionCatalog c = (ExtensionCatalog) o;
            final OriginPreference op = (OriginPreference) c.getMetadata().get("origin-preference");
            if (op == null) {
                continue;
            }
            if (eoBuilder == null) {
                eoBuilder = ExtensionOrigins.builder(e.getArtifact().getKey());
            }
            eoBuilder.addOrigin(c, op);
        }
        if (eoBuilder != null) {
            extOrigins.add(eoBuilder.build());
        }
    }

    private static Collection<Path> ensureResolvable(MavenArtifactResolver resolver, Artifact a) throws MojoExecutionException {
        final DependencyNode root;
        try {
            root = resolver.collectDependencies(a, Collections.emptyList()).getRoot();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to collect dependencies of " + a, e);
        }
        final List<Path> createdDirs = new ArrayList<>();
        ensureResolvableModule(root, resolver.getMavenContext().getWorkspace(), createdDirs);
        return createdDirs;
    }

    private static void ensureResolvableModule(DependencyNode node, LocalWorkspace workspace, List<Path> createdDirs)
            throws MojoExecutionException {
        Artifact artifact = node.getArtifact();
        if (artifact != null) {
            final LocalProject module = workspace.getProject(artifact.getGroupId(), artifact.getArtifactId());
            if (module != null && !module.getRawModel().getPackaging().equals(ArtifactCoords.TYPE_POM)) {
                final Path classesDir = module.getClassesDir();
                if (!Files.exists(classesDir)) {
                    Path topDirToCreate = classesDir;
                    while (!Files.exists(topDirToCreate.getParent())) {
                        topDirToCreate = topDirToCreate.getParent();
                    }
                    try {
                        Files.createDirectories(classesDir);
                        createdDirs.add(topDirToCreate);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to create " + classesDir, e);
                    }
                }
            }
        }
        for (DependencyNode c : node.getChildren()) {
            ensureResolvableModule(c, workspace, createdDirs);
        }
    }
}
