package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.workspace.DefaultProcessedSources;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.GAV;
import io.quarkus.paths.PathList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Resource;
import org.apache.maven.model.building.ModelBuildingResult;

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

    public PathsCollection getResourcesSourcesDirs() {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getResources();
        if (resources.isEmpty()) {
            return PathsCollection.of(resolveRelativeToBaseDir(null, "src/main/resources"));
        }
        return PathsCollection.from(resources.stream()
                .map(Resource::getDirectory)
                .map(resourcesDir -> resolveRelativeToBaseDir(resourcesDir, "src/main/resources"))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public PathsCollection getTestResourcesSourcesDirs() {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getTestResources();
        if (resources.isEmpty()) {
            return PathsCollection.of(resolveRelativeToBaseDir(null, "src/test/resources"));
        }
        return PathsCollection.from(resources.stream()
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
        final DefaultWorkspaceModule module = new DefaultWorkspaceModule(
                new GAV(getKey().getGroupId(), getKey().getArtifactId(), getVersion()), dir.toFile(), getOutputDir().toFile());
        module.addMainSources(new DefaultProcessedSources(getSourcesSourcesDir().toFile(), getClassesDir().toFile()));
        addMainResources(module);
        module.addTestSources(new DefaultProcessedSources(getTestSourcesSourcesDir().toFile(), getTestClassesDir().toFile()));
        addTestResources(module);
        module.setBuildFiles(PathList.of(getRawModel().getPomFile().toPath()));
        return module;
    }

    private void addMainResources(DefaultWorkspaceModule module) {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getResources();
        if (resources.isEmpty()) {
            module.addMainResources(new DefaultProcessedSources(
                    resolveRelativeToBaseDir(null, "src/main/resources").toFile(), getClassesDir().toFile()));
        } else {
            for (Resource r : resources) {
                module.addMainResources(
                        new DefaultProcessedSources(resolveRelativeToBaseDir(r.getDirectory(), "src/main/resources").toFile(),
                                (r.getTargetPath() == null ? getClassesDir()
                                        : getClassesDir()
                                                .resolve(stripProjectBasedirPrefix(r.getTargetPath(), PROJECT_OUTPUT_DIR)))
                                                        .toFile()));
            }
        }
    }

    private void addTestResources(DefaultWorkspaceModule module) {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getTestResources();
        if (resources.isEmpty()) {
            module.addTestResources(new DefaultProcessedSources(
                    resolveRelativeToBaseDir(null, "src/test/resources").toFile(), getTestClassesDir().toFile()));
        } else {
            for (Resource r : resources) {
                module.addTestResources(
                        new DefaultProcessedSources(resolveRelativeToBaseDir(r.getDirectory(), "src/test/resources").toFile(),
                                (r.getTargetPath() == null ? getTestClassesDir()
                                        : getTestClassesDir()
                                                .resolve(stripProjectBasedirPrefix(r.getTargetPath(), PROJECT_OUTPUT_DIR)))
                                                        .toFile()));
            }
        }
    }
}
