package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultSourceDir;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.paths.DirectoryPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.model.building.ModelBuildingResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *
 * @author Alexey Loubyansky
 */
public class LocalProject {

    public static final String PROJECT_GROUPID = "${project.groupId}";

    private static final String PROJECT_BASEDIR = "${project.basedir}";
    private static final String PROJECT_BUILD_DIR = "${project.build.directory}";
    private static final String PROJECT_OUTPUT_DIR = "${project.build.outputDirectory}";

    public static final String POM_XML = "pom.xml";

    public static LocalProject load(Path path) throws BootstrapMavenException {
        return load(path, true);
    }

    public static LocalProject load(Path path, boolean required) throws BootstrapMavenException {
        final Path pom = locateCurrentProjectPom(path, required);
        if (pom == null) {
            return null;
        }
        try {
            return new LocalProject(readModel(pom), null);
        } catch (UnresolvedVersionException e) {
            // if a property in the version couldn't be resolved, we are trying to resolve it from the workspace
            return loadWorkspace(pom);
        }
    }

    public static LocalProject loadWorkspace(Path path) throws BootstrapMavenException {
        return loadWorkspace(path, true);
    }

    public static LocalProject loadWorkspace(Path path, boolean required) throws BootstrapMavenException {
        try {
            return new WorkspaceLoader(null, path.normalize().toAbsolutePath()).load();
        } catch (Exception e) {
            if (required) {
                throw e;
            }
            return null;
        }
    }

    /**
     * Loads the workspace the current project belongs to.
     * If current project does not exist then the method will return null.
     *
     * @param ctx bootstrap maven context
     * @return current project with the workspace or null in case the current project could not be resolved
     * @throws BootstrapMavenException in case of an error
     */
    public static LocalProject loadWorkspace(BootstrapMavenContext ctx) throws BootstrapMavenException {
        final Path currentProjectPom = ctx.getCurrentProjectPomOrNull();
        if (currentProjectPom == null) {
            return null;
        }
        final Path rootProjectBaseDir = ctx.getRootProjectBaseDir();
        final WorkspaceLoader wsLoader = new WorkspaceLoader(ctx, currentProjectPom);

        if (rootProjectBaseDir != null && !rootProjectBaseDir.equals(currentProjectPom.getParent())) {
            wsLoader.setWorkspaceRootPom(rootProjectBaseDir.resolve(POM_XML));
        }
        return wsLoader.load();
    }

    static final Model readModel(Path pom) throws BootstrapMavenException {
        try {
            final Model model = ModelUtils.readModel(pom);
            model.setPomFile(pom.toFile());
            return model;
        } catch (IOException e) {
            throw new BootstrapMavenException("Failed to read " + pom, e);
        }
    }

    static Path locateCurrentProjectPom(Path path, boolean required) throws BootstrapMavenException {
        Path p = path;
        while (p != null) {
            final Path pom = p.resolve(POM_XML);
            if (Files.exists(pom)) {
                return pom;
            }
            p = p.getParent();
        }
        if (required) {
            throw new BootstrapMavenException("Failed to locate project pom.xml for " + path);
        }
        return null;
    }

    private final Model rawModel;
    private final String groupId;
    private final String artifactId;
    private String version;
    private final Path dir;
    private final LocalWorkspace workspace;
    final List<LocalProject> modules = new ArrayList<>(0);
    private AppArtifactKey key;
    private final ModelBuildingResult modelBuildingResult;
    private WorkspaceModule module;

    LocalProject(ModelBuildingResult modelBuildingResult, LocalWorkspace workspace) {
        this.rawModel = modelBuildingResult.getRawModel();
        final Model effectiveModel = modelBuildingResult.getEffectiveModel();
        this.groupId = effectiveModel.getGroupId();
        this.artifactId = effectiveModel.getArtifactId();
        this.version = effectiveModel.getVersion();
        this.dir = effectiveModel.getProjectDirectory().toPath();
        this.modelBuildingResult = modelBuildingResult;
        this.workspace = workspace;
        if (workspace != null) {
            workspace.addProject(this, rawModel.getPomFile().lastModified());
        }
    }

    LocalProject(Model rawModel, LocalWorkspace workspace) throws BootstrapMavenException {
        this.modelBuildingResult = null;
        this.rawModel = rawModel;
        this.dir = rawModel.getProjectDirectory().toPath();
        this.workspace = workspace;
        this.groupId = ModelUtils.getGroupId(rawModel);
        this.artifactId = rawModel.getArtifactId();

        final String rawVersion = ModelUtils.getRawVersion(rawModel);
        final boolean rawVersionIsUnresolved = ModelUtils.isUnresolvedVersion(rawVersion);
        version = rawVersionIsUnresolved ? ModelUtils.resolveVersion(rawVersion, rawModel) : rawVersion;

        if (workspace != null) {
            workspace.addProject(this, rawModel.getPomFile().lastModified());
            if (rawVersionIsUnresolved && version != null) {
                workspace.setResolvedVersion(version);
            }
        } else if (version == null && rawVersionIsUnresolved) {
            throw UnresolvedVersionException.forGa(groupId, artifactId, rawVersion);
        }
    }

    public LocalProject getLocalParent() {
        if (workspace == null) {
            return null;
        }
        final Parent parent = rawModel.getParent();
        if (parent == null) {
            return null;
        }
        return workspace.getProject(parent.getGroupId(), parent.getArtifactId());
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        if (version != null) {
            return version;
        }
        if (workspace != null) {
            version = workspace.getResolvedVersion();
        }
        if (version == null) {
            throw UnresolvedVersionException.forGa(groupId, artifactId, ModelUtils.getRawVersion(rawModel));
        }
        return version;
    }

    public Path getDir() {
        return dir;
    }

    public Path getOutputDir() {
        return modelBuildingResult == null
                ? resolveRelativeToBaseDir(configuredBuildDir(this, build -> build.getDirectory()), "target")
                : Paths.get(modelBuildingResult.getEffectiveModel().getBuild().getDirectory());
    }

    public Path getCodeGenOutputDir() {
        return getOutputDir().resolve("generated-sources");
    }

    public Path getClassesDir() {
        return modelBuildingResult == null
                ? resolveRelativeToBuildDir(configuredBuildDir(this, build -> build.getOutputDirectory()), "classes")
                : Paths.get(modelBuildingResult.getEffectiveModel().getBuild().getOutputDirectory());
    }

    public Path getTestClassesDir() {
        return modelBuildingResult == null
                ? resolveRelativeToBuildDir(configuredBuildDir(this, build -> build.getTestOutputDirectory()), "test-classes")
                : Paths.get(modelBuildingResult.getEffectiveModel().getBuild().getTestOutputDirectory());
    }

    public Path getSourcesSourcesDir() {
        return modelBuildingResult == null
                ? resolveRelativeToBaseDir(configuredBuildDir(this, build -> build.getSourceDirectory()), "src/main/java")
                : Paths.get(modelBuildingResult.getEffectiveModel().getBuild().getSourceDirectory());
    }

    public Path getTestSourcesSourcesDir() {
        return resolveRelativeToBaseDir(configuredBuildDir(this, build -> build.getTestSourceDirectory()), "src/test/java");
    }

    public Path getSourcesDir() {
        return getSourcesSourcesDir().getParent();
    }

    public PathCollection getResourcesSourcesDirs() {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getResources();
        if (resources.isEmpty()) {
            return PathList.of(resolveRelativeToBaseDir(null, "src/main/resources"));
        }
        return PathList.from(resources.stream()
                .map(Resource::getDirectory)
                .map(resourcesDir -> resolveRelativeToBaseDir(resourcesDir, "src/main/resources"))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public PathCollection getTestResourcesSourcesDirs() {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getTestResources();
        if (resources.isEmpty()) {
            return PathList.of(resolveRelativeToBaseDir(null, "src/test/resources"));
        }
        return PathList.from(resources.stream()
                .map(Resource::getDirectory)
                .map(resourcesDir -> resolveRelativeToBaseDir(resourcesDir, "src/test/resources"))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public ModelBuildingResult getModelBuildingResult() {
        return modelBuildingResult;
    }

    public Model getRawModel() {
        return rawModel;
    }

    public LocalWorkspace getWorkspace() {
        return workspace;
    }

    public AppArtifactKey getKey() {
        return key == null ? key = new AppArtifactKey(groupId, artifactId) : key;
    }

    public AppArtifact getAppArtifact() {
        return getAppArtifact(
                modelBuildingResult == null ? rawModel.getPackaging() : modelBuildingResult.getEffectiveModel().getPackaging());
    }

    public AppArtifact getAppArtifact(String extension) {
        return new AppArtifact(groupId, artifactId, "", extension, getVersion());
    }

    public Path resolveRelativeToBaseDir(String path) {
        return resolveRelativeToBaseDir(path, null);
    }

    private Path resolveRelativeToBaseDir(String path, String defaultPath) {
        return dir.resolve(path == null ? defaultPath : stripProjectBasedirPrefix(path, PROJECT_BASEDIR));
    }

    private Path resolveRelativeToBuildDir(String path, String defaultPath) {
        return getOutputDir().resolve(path == null ? defaultPath : stripProjectBasedirPrefix(path, PROJECT_BUILD_DIR));
    }

    private static String stripProjectBasedirPrefix(String path, String expr) {
        return path.startsWith(expr) ? path.substring(expr.length() + 1) : path;
    }

    private static String configuredBuildDir(LocalProject project, Function<Build, String> f) {
        String dir = project.rawModel.getBuild() == null ? null : f.apply(project.rawModel.getBuild());
        while (dir == null) {
            project = project.getLocalParent();
            if (project == null) {
                break;
            }
            if (project.rawModel.getBuild() != null) {
                dir = f.apply(project.rawModel.getBuild());
            }
        }
        return dir;
    }

    public WorkspaceModule toWorkspaceModule() {
        if (module != null) {
            return module;
        }
        final DefaultWorkspaceModule module = new DefaultWorkspaceModule(
                new GAV(getKey().getGroupId(), getKey().getArtifactId(), getVersion()), dir.toFile(), getOutputDir().toFile());

        final Build build = (modelBuildingResult == null ? getRawModel() : modelBuildingResult.getEffectiveModel()).getBuild();
        if (build != null && !build.getPlugins().isEmpty()) {
            for (Plugin plugin : build.getPlugins()) {
                if (!plugin.getArtifactId().equals("maven-jar-plugin")) {
                    continue;
                }
                for (PluginExecution e : plugin.getExecutions()) {
                    DefaultArtifactSources src = null;
                    if (e.getGoals().contains(ArtifactCoords.TYPE_JAR)) {
                        src = processJarPluginExecutionConfig(e, false);
                    } else if (e.getGoals().contains("test-jar")) {
                        src = processJarPluginExecutionConfig(e, true);
                    }
                    if (src != null) {
                        module.addArtifactSources(src);
                    }
                }
            }
        }

        if (module.getMainSources() == null) {
            module.addArtifactSources(new DefaultArtifactSources(DefaultWorkspaceModule.MAIN,
                    Collections.singletonList(new DefaultSourceDir(getSourcesSourcesDir().toFile(), getClassesDir().toFile())),
                    collectMainResources(null)));
        }
        if (module.getTestSources() == null) {
            module.addArtifactSources(new DefaultArtifactSources(DefaultWorkspaceModule.TEST,
                    Collections.singletonList(
                            new DefaultSourceDir(getTestSourcesSourcesDir().toFile(), getTestClassesDir().toFile())),
                    collectTestResources(null)));
        }
        module.setBuildFiles(PathList.of(getRawModel().getPomFile().toPath()));

        return this.module = module;
    }

    private DefaultArtifactSources processJarPluginExecutionConfig(PluginExecution e, boolean test) {
        final Object config = e.getConfiguration();
        if (config == null || !(config instanceof Xpp3Dom)) {
            return null;
        }
        Xpp3Dom dom = (Xpp3Dom) config;
        final List<String> includes = collectChildValues(dom.getChild("includes"));
        final List<String> excludes = collectChildValues(dom.getChild("excludes"));
        if (includes == null && excludes == null) {
            return null;
        }
        final PathFilter filter = new PathFilter(includes, excludes);
        final String classifier = getClassifier(dom, test);
        final Collection<SourceDir> sources = Collections.singletonList(
                new DefaultSourceDir(new DirectoryPathTree(test ? getTestSourcesSourcesDir() : getSourcesSourcesDir()),
                        new DirectoryPathTree(test ? getTestClassesDir() : getClassesDir(), filter),
                        Collections.emptyMap()));
        final Collection<SourceDir> resources = test ? collectTestResources(filter) : collectMainResources(filter);
        return new DefaultArtifactSources(classifier, sources, resources);
    }

    private List<String> collectChildValues(final Xpp3Dom container) {
        if (container == null) {
            return null;
        }
        final Xpp3Dom[] excludeElements = container.getChildren();
        final List<String> list = new ArrayList<>(excludeElements.length);
        for (Xpp3Dom child : container.getChildren()) {
            list.add(child.getValue());
        }
        return list;
    }

    private static String getClassifier(Xpp3Dom dom, boolean test) {
        final Xpp3Dom classifier = dom.getChild("classifier");
        return classifier == null ? (test ? DefaultWorkspaceModule.TEST : DefaultWorkspaceModule.MAIN) : classifier.getValue();
    }

    private Collection<SourceDir> collectMainResources(PathFilter filter) {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getResources();
        if (resources.isEmpty()) {
            return Collections.singletonList(new DefaultSourceDir(
                    new DirectoryPathTree(resolveRelativeToBaseDir(null, "src/main/resources")),
                    new DirectoryPathTree(getClassesDir(), filter), Collections.emptyMap()));
        }
        final List<SourceDir> sourceDirs = new ArrayList<>(resources.size());
        for (Resource r : resources) {
            sourceDirs.add(
                    new DefaultSourceDir(
                            new DirectoryPathTree(resolveRelativeToBaseDir(r.getDirectory(), "src/main/resources")),
                            new DirectoryPathTree((r.getTargetPath() == null ? getClassesDir()
                                    : getClassesDir()
                                            .resolve(stripProjectBasedirPrefix(r.getTargetPath(), PROJECT_OUTPUT_DIR))),
                                    filter),
                            Collections.emptyMap()));
        }
        return sourceDirs;
    }

    private Collection<SourceDir> collectTestResources(PathFilter filter) {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getTestResources();
        if (resources.isEmpty()) {
            return Collections.singletonList(new DefaultSourceDir(
                    new DirectoryPathTree(resolveRelativeToBaseDir(null, "src/test/resources")),
                    new DirectoryPathTree(getTestClassesDir(), filter), Collections.emptyMap()));
        }
        final List<SourceDir> sourceDirs = new ArrayList<>(resources.size());
        for (Resource r : resources) {
            sourceDirs.add(
                    new DefaultSourceDir(
                            new DirectoryPathTree(resolveRelativeToBaseDir(r.getDirectory(), "src/test/resources")),
                            new DirectoryPathTree((r.getTargetPath() == null ? getTestClassesDir()
                                    : getTestClassesDir()
                                            .resolve(stripProjectBasedirPrefix(r.getTargetPath(), PROJECT_OUTPUT_DIR))),
                                    filter),
                            Collections.emptyMap()));
        }
        return sourceDirs;
    }
}
