package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Resource;

/**
 *
 * @author Alexey Loubyansky
 */
public class LocalProject {

    public static final String PROJECT_GROUPID = "${project.groupId}";

    private static final String PROJECT_BASEDIR = "${project.basedir}";
    private static final String POM_XML = "pom.xml";

    private static class WorkspaceLoader {

        private final LocalWorkspace workspace = new LocalWorkspace();
        private final Map<Path, Model> cachedModels = new HashMap<>();
        private final Path currentProjectPom;
        private Path workspaceRootPom;

        private WorkspaceLoader(Path currentProjectPom) throws BootstrapMavenException {
            this.currentProjectPom = isPom(currentProjectPom) ? currentProjectPom
                    : locateCurrentProjectPom(currentProjectPom, true);
        }

        private boolean isPom(Path p) {
            if (Files.exists(p) && !Files.isDirectory(p)) {
                try {
                    loadAndCache(p);
                    return true;
                } catch (BootstrapMavenException e) {
                    // not a POM file
                }
            }
            return false;
        }

        private Model model(Path pomFile) throws BootstrapMavenException {
            Model model = cachedModels.get(pomFile.getParent());
            if (model == null) {
                model = loadAndCache(pomFile);
            }
            return model;
        }

        private Model loadAndCache(Path pomFile) throws BootstrapMavenException {
            final Model model = readModel(pomFile);
            cachedModels.put(pomFile.getParent(), model);
            return model;
        }

        void setWorkspaceRootPom(Path rootPom) {
            this.workspaceRootPom = rootPom;
        }

        private Path getWorkspaceRootPom() throws BootstrapMavenException {
            return workspaceRootPom == null ? workspaceRootPom = resolveWorkspaceRootPom() : workspaceRootPom;
        }

        private Path resolveWorkspaceRootPom() throws BootstrapMavenException {
            Path rootPom = null;
            Path projectPom = currentProjectPom;
            Model model = model(projectPom);
            do {
                rootPom = projectPom;
                final Parent parent = model.getParent();
                if (parent != null
                        && parent.getRelativePath() != null
                        && !parent.getRelativePath().isEmpty()) {
                    projectPom = projectPom.getParent().resolve(parent.getRelativePath()).normalize();
                    if (Files.isDirectory(projectPom)) {
                        projectPom = projectPom.resolve(POM_XML);
                    }
                } else {
                    final Path parentDir = projectPom.getParent().getParent();
                    if (parentDir == null) {
                        break;
                    }
                    projectPom = parentDir.resolve(POM_XML);
                }
                model = null;
                if (Files.exists(projectPom)) {
                    model = cachedModels.get(projectPom.getParent());
                    if (model == null) {
                        model = loadAndCache(projectPom);
                    } else {
                        // if the parent is not at the top of the FS tree, it might have already been parsed
                        model = null;
                        for (Map.Entry<Path, Model> entry : cachedModels.entrySet()) {
                            // we are looking for the root dir of the workspace
                            if (rootPom.getNameCount() > entry.getKey().getNameCount()) {
                                rootPom = entry.getValue().getPomFile().toPath();
                            }
                        }
                    }
                }
            } while (model != null);
            return rootPom;
        }

        LocalProject load() throws BootstrapMavenException {
            load(null, getWorkspaceRootPom());
            if (workspace.getCurrentProject() == null) {
                if (!currentProjectPom.equals(getWorkspaceRootPom())) {
                    load(null, currentProjectPom);
                }
                if (workspace.getCurrentProject() == null) {
                    throw new BootstrapMavenException(
                            "Failed to locate project " + currentProjectPom + " in the loaded workspace");
                }
            }
            return workspace.getCurrentProject();
        }

        private void load(LocalProject parent, Path pom) throws BootstrapMavenException {
            final Model model = model(pom);
            final LocalProject project = new LocalProject(model, workspace);
            if (parent != null) {
                parent.modules.add(project);
            }
            if (workspace.getCurrentProject() == null && currentProjectPom.getParent().equals(project.getDir())) {
                workspace.setCurrentProject(project);
            }
            final List<String> modules = project.getRawModel().getModules();
            if (!modules.isEmpty()) {
                for (String module : modules) {
                    load(project, project.getDir().resolve(module).resolve(POM_XML));
                }
            }
        }
    }

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
            return new WorkspaceLoader(path.normalize().toAbsolutePath()).load();
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
        final WorkspaceLoader wsLoader = new WorkspaceLoader(currentProjectPom);
        if (rootProjectBaseDir != null && !rootProjectBaseDir.equals(currentProjectPom.getParent())) {
            wsLoader.setWorkspaceRootPom(rootProjectBaseDir.resolve(POM_XML));
        }
        final LocalProject lp = wsLoader.load();
        lp.getWorkspace().setBootstrapMavenContext(ctx);
        return lp;
    }

    private static final Model readModel(Path pom) throws BootstrapMavenException {
        try {
            final Model model = ModelUtils.readModel(pom);
            model.setPomFile(pom.toFile());
            return model;
        } catch (IOException e) {
            throw new BootstrapMavenException("Failed to read " + pom, e);
        }
    }

    private static Path locateCurrentProjectPom(Path path, boolean required) throws BootstrapMavenException {
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
    private final String version;
    private final Path dir;
    private final LocalWorkspace workspace;
    private final List<LocalProject> modules = new ArrayList<>(0);
    private AppArtifactKey key;

    private LocalProject(Model rawModel, LocalWorkspace workspace) throws BootstrapMavenException {
        this.rawModel = rawModel;
        this.dir = rawModel.getProjectDirectory().toPath();
        this.workspace = workspace;
        this.groupId = ModelUtils.getGroupId(rawModel);
        this.artifactId = rawModel.getArtifactId();

        final String rawVersion = ModelUtils.getRawVersion(rawModel);
        final boolean rawVersionIsUnresolved = ModelUtils.isUnresolvedVersion(rawVersion);
        String resolvedVersion = rawVersionIsUnresolved ? ModelUtils.resolveVersion(rawVersion, rawModel) : rawVersion;

        if (workspace != null) {
            workspace.addProject(this, rawModel.getPomFile().lastModified());
            if (rawVersionIsUnresolved) {
                if (resolvedVersion == null) {
                    resolvedVersion = workspace.getResolvedVersion();
                    if (resolvedVersion == null) {
                        throw UnresolvedVersionException.forGa(groupId, artifactId, rawVersion);
                    }
                } else {
                    workspace.setResolvedVersion(resolvedVersion);
                }
            }
        } else if (resolvedVersion == null) {
            throw UnresolvedVersionException.forGa(groupId, artifactId, rawVersion);
        }

        this.version = resolvedVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public Path getDir() {
        return dir;
    }

    public Path getOutputDir() {
        return dir.resolve("target");
    }

    public Path getCodeGenOutputDir() {
        return dir.resolve("target").resolve("generated-sources");
    }

    public Path getClassesDir() {
        final String classesDir = rawModel.getBuild() == null ? null : rawModel.getBuild().getOutputDirectory();
        return resolveRelativeToBaseDir(classesDir, "target/classes");
    }

    public Path getTestClassesDir() {
        final String classesDir = rawModel.getBuild() == null ? null : rawModel.getBuild().getTestOutputDirectory();
        return resolveRelativeToBaseDir(classesDir, "target/test-classes");
    }

    public Path getSourcesSourcesDir() {
        final String srcDir = rawModel.getBuild() == null ? null : rawModel.getBuild().getSourceDirectory();
        return resolveRelativeToBaseDir(srcDir, "src/main/java");
    }

    public Path getSourcesDir() {
        final String srcDir = rawModel.getBuild() == null ? null : rawModel.getBuild().getSourceDirectory();
        return resolveRelativeToBaseDir(srcDir, "src/main/");
    }

    public Path getResourcesSourcesDir() {
        final List<Resource> resources = rawModel.getBuild() == null ? Collections.emptyList()
                : rawModel.getBuild().getResources();
        //todo: support multiple resources dirs for config hot deployment
        final String resourcesDir = resources.isEmpty() ? null : resources.get(0).getDirectory();
        return resolveRelativeToBaseDir(resourcesDir, "src/main/resources");
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
        return getAppArtifact(rawModel.getPackaging());
    }

    public AppArtifact getAppArtifact(String extension) {
        return new AppArtifact(groupId, artifactId, "", extension, version);
    }

    private Path resolveRelativeToBaseDir(String path, String defaultPath) {
        return dir.resolve(path == null ? defaultPath : stripProjectBasedirPrefix(path));
    }

    private static String stripProjectBasedirPrefix(String path) {
        return path.startsWith(PROJECT_BASEDIR) ? path.substring(PROJECT_BASEDIR.length() + 1) : path;
    }
}
