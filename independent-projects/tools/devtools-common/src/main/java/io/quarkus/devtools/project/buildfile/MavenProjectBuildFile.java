package io.quarkus.devtools.project.buildfile;

import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.codestartLoadersBuilder;
import static io.quarkus.devtools.project.extensions.Extensions.toKey;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.util.PlatformArtifacts;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

public class MavenProjectBuildFile extends BuildFile {

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(.+)}");

    public static QuarkusProject getProject(Path projectDir, MessageWriter log, Supplier<String> defaultQuarkusVersion)
            throws RegistryResolutionException {
        final MavenArtifactResolver mvnResolver = getMavenResolver(projectDir);
        final LocalProject currentProject = mvnResolver.getMavenContext().getCurrentProject();
        final Model projectModel;
        final Artifact projectPom;
        if (currentProject != null && isSameFile(projectDir, currentProject.getDir())) {
            projectPom = new DefaultArtifact(currentProject.getGroupId(), currentProject.getArtifactId(), null, "pom",
                    currentProject.getVersion());
            projectModel = currentProject.getRawModel();
        } else {
            projectPom = null;
            projectModel = null;
        }
        return getProject(projectPom, projectModel, projectDir,
                projectModel == null ? new Properties() : projectModel.getProperties(), mvnResolver, log,
                defaultQuarkusVersion);
    }

    public static QuarkusProject getProject(Artifact projectPom, Model projectModel, Path projectDir,
            Properties projectProps, MavenArtifactResolver mvnResolver, MessageWriter log,
            Supplier<String> defaultQuarkusVersion) throws RegistryResolutionException {
        final List<ArtifactCoords> managedDeps;
        final Supplier<List<ArtifactCoords>> deps;
        final List<ArtifactCoords> importedPlatforms;
        final String quarkusVersion;
        if (projectPom == null) {
            managedDeps = Collections.emptyList();
            deps = () -> Collections.emptyList();
            importedPlatforms = Collections.emptyList();
            // TODO allow multiple streams in the same catalog for now
            quarkusVersion = null;// defaultQuarkusVersion.get();
        } else {
            final ArtifactDescriptorResult descriptor = describe(mvnResolver, projectPom);
            managedDeps = toArtifactCoords(descriptor.getManagedDependencies());
            deps = () -> toArtifactCoords(descriptor.getDependencies());
            importedPlatforms = collectPlatformDescriptors(managedDeps, log);
            quarkusVersion = getQuarkusVersion(managedDeps);
        }

        final ExtensionCatalog extensionCatalog;

        final ArtifactCoords fallbackVersion = ExtensionCatalogResolver.toPlatformBom(quarkusVersion == null
                ? defaultQuarkusVersion.get()
                : quarkusVersion);

        final ExtensionCatalogResolver catalogResolver = ExtensionCatalogResolver.builder()
                .withRegistryClient(QuarkusProjectHelper.isRegistryClientEnabled())
                .withLog(log)
                .withResolver(mvnResolver)
                .withFallbackVersion(fallbackVersion)
                .build();

        if (importedPlatforms.isEmpty()) {
            extensionCatalog = quarkusVersion == null
                    ? catalogResolver.resolveExtensionCatalog()
                    : catalogResolver.resolveExtensionCatalog(quarkusVersion);
        } else {
            extensionCatalog = catalogResolver.resolveExtensionCatalog(importedPlatforms);
        }

        final MavenProjectBuildFile extensionManager = new MavenProjectBuildFile(projectDir, extensionCatalog,
                projectModel, deps, managedDeps, projectProps, projectPom == null ? null : mvnResolver);

        final List<ResourceLoader> codestartResourceLoaders = codestartLoadersBuilder().catalog(extensionCatalog)
                .artifactResolver(mvnResolver).build();

        return QuarkusProject.builder()
                .projectDir(projectDir)
                .extensionCatalog(extensionCatalog)
                .codestartResourceLoaders(codestartResourceLoaders)
                .extensionManager(extensionManager)
                .log(log)
                .build();
    }

    private static MavenArtifactResolver getMavenResolver(Path projectDir) {
        final RegistriesConfig toolsConfig = QuarkusProjectHelper.toolsConfig();
        try {
            return MavenArtifactResolver.builder()
                    .setArtifactTransferLogging(toolsConfig.isDebug())
                    .setCurrentProject(projectDir.toAbsolutePath().toString())
                    .setPreferPomsFromWorkspace(true)
                    .build();
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to initialize Maven artifact resolver", e);
        }
    }

    private static String getQuarkusVersion(List<ArtifactCoords> managedDeps) {
        for (ArtifactCoords a : managedDeps) {
            if (a.getArtifactId().endsWith("quarkus-core") && a.getGroupId().equals("io.quarkus")) {
                return a.getVersion();
            }
        }
        return null;
    }

    private static List<ArtifactCoords> toArtifactCoords(List<org.eclipse.aether.graph.Dependency> deps) {
        final List<ArtifactCoords> result = new ArrayList<>(deps.size());
        for (org.eclipse.aether.graph.Dependency dep : deps) {
            org.eclipse.aether.artifact.Artifact a = dep.getArtifact();
            result.add(new ArtifactCoords(a.getGroupId(), a.getArtifactId(), a.getClassifier(),
                    a.getExtension(), a.getVersion()));
        }
        return result;
    }

    private static ArtifactDescriptorResult describe(MavenArtifactResolver resolver, Artifact projectArtifact) {
        try {
            return resolver.resolveDescriptor(projectArtifact);
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to resolve descriptor for " + projectArtifact, e);
        }
    }

    private static List<ArtifactCoords> collectPlatformDescriptors(List<ArtifactCoords> managedDeps, MessageWriter log) {
        if (managedDeps.isEmpty()) {
            return Collections.emptyList();
        }
        final List<ArtifactCoords> result = new ArrayList<>(4);
        for (ArtifactCoords c : managedDeps) {
            if (PlatformArtifacts.isCatalogArtifact(c)) {
                result.add(c);
            }
        }
        return result;
    }

    private static boolean isSameFile(Path p1, Path p2) {
        try {
            return Files.isSameFile(p1, p2);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compare " + p1 + " to " + p2, e);
        }
    }

    private Model model;
    private List<ArtifactCoords> managedDependencies;
    private Properties projectProps;
    private Supplier<List<ArtifactCoords>> projectDepsSupplier;
    private List<ArtifactCoords> dependencies;
    private List<ArtifactCoords> importedPlatforms;
    private MavenArtifactResolver resolver;

    private MavenProjectBuildFile(Path projectDirPath, ExtensionCatalog extensionsCatalog, Model model,
            Supplier<List<ArtifactCoords>> projectDeps,
            List<ArtifactCoords> projectManagedDeps,
            Properties projectProps, MavenArtifactResolver resolver) {
        super(projectDirPath, extensionsCatalog);
        this.model = model;
        this.projectDepsSupplier = projectDeps;
        this.managedDependencies = projectManagedDeps;
        this.projectProps = projectProps;
        this.resolver = resolver;
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.MAVEN;
    }

    @Override
    protected boolean importBom(ArtifactCoords coords) {
        if (!"pom".equalsIgnoreCase(coords.getType())) {
            throw new IllegalArgumentException(coords + " is not a POM");
        }
        final String depKey = depKey(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getType());
        if (coords.getGroupId().equals(getProperty("quarkus.platform.group-id"))
                && coords.getVersion().equals(getProperty("quarkus.platform.version"))) {
            coords = new ArtifactCoords("${quarkus.platform.group-id}",
                    coords.getArtifactId().equals(getProperty("quarkus.platform.artifact-id"))
                            ? "${quarkus.platform.artifact-id}"
                            : coords.getArtifactId(),
                    "pom", "${quarkus.platform.version}");
        }

        final Dependency d = new Dependency();
        d.setGroupId(coords.getGroupId());
        d.setArtifactId(coords.getArtifactId());
        d.setType(coords.getType());
        d.setScope("import");
        d.setVersion(coords.getVersion());
        DependencyManagement dependencyManagement = model().getDependencyManagement();
        if (dependencyManagement == null) {
            dependencyManagement = new DependencyManagement();
            model().setDependencyManagement(dependencyManagement);
        }
        if (dependencyManagement.getDependencies()
                .stream()
                .filter(t -> t.getScope().equals("import"))
                .noneMatch(thisDep -> depKey.equals(resolveKey(thisDep)))) {
            dependencyManagement.addDependency(d);
            // the effective managed dependencies set may already include it
            if (!getManagedDependencies().contains(coords)) {
                getManagedDependencies().add(coords);
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean addDependency(ArtifactCoords coords, boolean managed) {
        final Dependency d = new Dependency();
        d.setGroupId(coords.getGroupId());
        d.setArtifactId(coords.getArtifactId());
        if (!managed) {
            d.setVersion(coords.getVersion());
        }
        // When classifier is empty, you get  <classifier></classifier> in the pom.xml
        if (coords.getClassifier() != null && !coords.getClassifier().isEmpty()) {
            d.setClassifier(coords.getClassifier());
        }
        d.setType(coords.getType());
        if ("pom".equalsIgnoreCase(coords.getType())) {
            d.setScope("import");
            DependencyManagement dependencyManagement = model().getDependencyManagement();
            if (dependencyManagement == null) {
                dependencyManagement = new DependencyManagement();
                model().setDependencyManagement(dependencyManagement);
            }
            if (dependencyManagement.getDependencies()
                    .stream()
                    .noneMatch(thisDep -> d.getManagementKey().equals(resolveKey(thisDep)))) {
                dependencyManagement.addDependency(d);
                // the effective managed dependencies set may already include it
                if (!getManagedDependencies().contains(coords)) {
                    getManagedDependencies().add(coords);
                }
                return true;
            }
        } else if (model().getDependencies()
                .stream()
                .noneMatch(thisDep -> d.getManagementKey().equals(thisDep.getManagementKey()))) {
            final int index = getIndexToAddExtension();
            if (index >= 0) {
                model().getDependencies().add(index, d);
            } else {
                model().getDependencies().add(d);
            }

            // it could still be a transitive dependency or inherited from the parent
            if (!getDependencies().contains(coords)) {
                getDependencies().add(coords);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void removeDependency(ArtifactKey key) throws IOException {
        if (model() != null) {
            final Iterator<ArtifactCoords> i = getDependencies().iterator();
            while (i.hasNext()) {
                final ArtifactCoords a = i.next();
                if (a.getKey().equals(key)) {
                    i.remove();
                    break;
                }
            }
            model().getDependencies().removeIf(d -> Objects.equals(toKey(d), key));
        }
    }

    @Override
    protected List<ArtifactCoords> getDependencies() {
        if (dependencies == null) {
            dependencies = projectDepsSupplier.get();
            projectDepsSupplier = null;
        }
        return dependencies;
    }

    @Override
    public final Collection<ArtifactCoords> getInstalledPlatforms() throws IOException {
        if (importedPlatforms == null) {
            final List<ArtifactCoords> tmp = new ArrayList<>();
            for (ArtifactCoords c : getManagedDependencies()) {
                if (PlatformArtifacts.isCatalogArtifact(c)) {
                    tmp.add(PlatformArtifacts.getBomArtifactForCatalog(c));
                }
            }
            importedPlatforms = tmp;
        }
        return importedPlatforms;
    }

    protected List<ArtifactCoords> getManagedDependencies() {
        return managedDependencies;
    }

    @Override
    protected void writeToDisk() throws IOException {
        if (model == null) {
            return;
        }
        try (ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream()) {
            MojoUtils.write(model(), pomOutputStream);
            writeToProjectFile(BuildTool.MAVEN.getDependenciesFile(), pomOutputStream.toByteArray());
        }
    }

    @Override
    protected String getProperty(String propertyName) {
        return projectProps.getProperty(propertyName);
    }

    @Override
    protected void refreshData() {
        final Path projectPom = getProjectDirPath().resolve("pom.xml");
        if (!Files.exists(projectPom)) {
            return;
        }
        try {
            model = ModelUtils.readModel(projectPom);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + projectPom, e);
        }
        projectProps = model.getProperties();
        final ArtifactDescriptorResult descriptor = describe(resolver(), new DefaultArtifact(
                ModelUtils.getGroupId(model), model.getArtifactId(), "pom", ModelUtils.getVersion(model)));
        managedDependencies = toArtifactCoords(descriptor.getManagedDependencies());
        projectDepsSupplier = () -> toArtifactCoords(descriptor.getDependencies());
        dependencies = null;
    }

    private MavenArtifactResolver resolver() {
        return resolver == null ? resolver = getMavenResolver(getProjectDirPath()) : resolver;
    }

    private int getIndexToAddExtension() {
        final List<Dependency> dependencies = model().getDependencies();
        for (int i = 0; i < dependencies.size(); i++) {
            if ("test".equals(dependencies.get(i).getScope())) {
                return i;
            }
        }
        return -1;
    }

    private Model model() {
        return model;
    }

    /**
     * Resolves dependencies containing property references in the GAV
     */
    private String resolveKey(Dependency dependency) {
        String resolvedGroupId = toResolvedProperty(dependency.getGroupId());
        String resolvedArtifactId = toResolvedProperty(dependency.getArtifactId());
        String resolvedVersion = toResolvedProperty(dependency.getVersion());
        if (!resolvedGroupId.equals(dependency.getGroupId())
                || !resolvedArtifactId.equals(dependency.getArtifactId())
                || !resolvedVersion.equals(dependency.getVersion())) {
            return depKey(resolvedGroupId, resolvedArtifactId, dependency.getClassifier(), dependency.getType());
        }
        return dependency.getManagementKey();
    }

    private static String depKey(String groupId, String artifactId, String classifier, String type) {
        final StringBuilder buf = new StringBuilder();
        buf.append(groupId).append(':').append(artifactId).append(':').append(type);
        if (classifier != null && !classifier.isEmpty()) {
            buf.append(':').append(classifier);
        }
        return buf.toString();
    }

    /**
     * Resolves properties as ${quarkus.platform.version}
     */
    private String toResolvedProperty(String value) {
        Matcher matcher = PROPERTY_PATTERN.matcher(value);
        if (matcher.matches()) {
            String property = getProperty(matcher.group(1));
            return property == null ? value : property;
        }
        return value;
    }
}
