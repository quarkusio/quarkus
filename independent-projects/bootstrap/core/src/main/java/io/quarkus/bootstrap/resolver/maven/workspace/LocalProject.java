package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Resource;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;

/**
 *
 * @author Alexey Loubyansky
 */
public class LocalProject {

    public static final String PROJECT_GROUPID = "${project.groupId}";

    private static final String PROJECT_BASEDIR = "${project.basedir}";
    private static final String POM_XML = "pom.xml";

    public static LocalProject load(Path path) throws BootstrapException {
        return new LocalProject(readModel(locateCurrentProjectDir(path).resolve(POM_XML)), null);
    }

    public static LocalProject loadWorkspace(Path path) throws BootstrapException {
        final Path currentProjectDir = locateCurrentProjectDir(path);
        final LocalWorkspace ws = new LocalWorkspace();
        final LocalProject project = load(ws, null, loadRootModel(currentProjectDir), currentProjectDir);
        return project == null ? load(ws, null, readModel(currentProjectDir.resolve(POM_XML)), currentProjectDir) : project;
    }

    private static LocalProject load(LocalWorkspace workspace, LocalProject parent, Model model, Path currentProjectDir) throws BootstrapException {
        final LocalProject project = new LocalProject(model, workspace);
        if(parent != null) {
            parent.modules.add(project);
        }
        LocalProject result = currentProjectDir == null || !currentProjectDir.equals(project.getDir()) ? null : project;
        final List<String> modules = project.getRawModel().getModules();
        if (!modules.isEmpty()) {
            Path dirArg = result == null ? currentProjectDir : null;
            for (String module : modules) {
                final LocalProject loaded = load(workspace, project, readModel(project.getDir().resolve(module).resolve(POM_XML)), dirArg);
                if(loaded != null && result == null) {
                    result = loaded;
                    dirArg = null;
                }
            }
        }
        return result;
    }

    private static Model loadRootModel(Path currentProjectDir) throws BootstrapException {
        Path pomXml = currentProjectDir.resolve(POM_XML);
        Model model = readModel(pomXml);
        Parent parent = model.getParent();
        while(parent != null) {
            if(parent.getRelativePath() != null && !parent.getRelativePath().isEmpty()) {
                pomXml = pomXml.getParent().resolve(parent.getRelativePath()).normalize();
                if(!Files.exists(pomXml)) {
                    return model;
                }
                if(Files.isDirectory(pomXml)) {
                    pomXml = pomXml.resolve(POM_XML);
                }
            } else {
                pomXml = pomXml.getParent().getParent().resolve(POM_XML);
                if(!Files.exists(pomXml)) {
                    return model;
                }
            }
            model = readModel(pomXml);
            parent = model.getParent();
        }
        return model;
    }

    private static final Model readModel(Path pom) throws BootstrapException {
        try {
            final Model model = ModelUtils.readModel(pom);
            model.setPomFile(pom.toFile());
            return model;
        } catch (IOException e) {
            throw new BootstrapException("Failed to read " + pom, e);
        }
    }

    private static Path locateCurrentProjectDir(Path path) throws BootstrapException {
        Path p = path;
        while(p != null) {
            if(Files.exists(p.resolve(POM_XML))) {
                return p;
            }
            p = p.getParent();
        }
        throw new BootstrapException("Failed to locate project pom.xml for " + path);
    }

    private final Model rawModel;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Path dir;
    private final LocalWorkspace workspace;
    private final List<LocalProject> modules = new ArrayList<>(0);

    private LocalProject(Model rawModel, LocalWorkspace workspace) throws BootstrapException {
        this.rawModel = rawModel;
        this.dir = rawModel.getProjectDirectory().toPath();
        this.workspace = workspace;
        final Parent parent = rawModel.getParent();
        String groupId = rawModel.getGroupId();
        if(groupId == null) {
            if(parent == null) {
                throw new BootstrapException("Failed to determine groupId for " + rawModel.getPomFile());
            }
            this.groupId = parent.getGroupId();
        } else {
            this.groupId = groupId;
        }

        this.artifactId = rawModel.getArtifactId();
        String version = rawModel.getVersion();
        if(version == null) {
            if(parent == null) {
                throw new BootstrapException("Failed to determine version for " + rawModel.getPomFile());
            }
            this.version = parent.getVersion();
        } else {
            this.version = version;
        }
        if(workspace != null) {
            workspace.addProject(this, rawModel.getPomFile().lastModified());
        }
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

    public Path getClassesDir() {
        return getOutputDir().resolve("classes");
    }

    public Path getSourcesSourcesDir() {
        if (getRawModel().getBuild() != null && getRawModel().getBuild().getSourceDirectory() != null) {
            String originalValue = getRawModel().getBuild().getSourceDirectory();
            return Paths.get(originalValue.startsWith(PROJECT_BASEDIR) ? originalValue.replace(PROJECT_BASEDIR, this.dir.toString()) : originalValue);
        }
        return dir.resolve("src/main/java");
    }

    public Path getResourcesSourcesDir() {
        if(getRawModel().getBuild() != null && getRawModel().getBuild().getResources() != null) {
            for (Resource i : getRawModel().getBuild().getResources()) {
                //todo: support multiple resources dirs for config hot deployment
                return Paths.get(i.getDirectory());
            }
        }
        return dir.resolve("src/main/resources");
    }

    public Model getRawModel() {
        return rawModel;
    }

    public LocalWorkspace getWorkspace() {
        return workspace;
    }

    public AppArtifactKey getKey() {
        return new AppArtifactKey(groupId, artifactId);
    }

    public AppArtifact getAppArtifact() {
        final AppArtifact appArtifact = new AppArtifact(groupId, artifactId, BootstrapConstants.EMPTY, rawModel.getPackaging(), version);
        appArtifact.setPath(getClassesDir());
        return appArtifact;
    }

    public List<LocalProject> getSelfWithLocalDeps() {
        if(workspace == null) {
            return Collections.singletonList(this);
        }
        final List<LocalProject> ordered = new ArrayList<>();
        collectSelfWithLocalDeps(this, new HashSet<>(),  ordered);
        return ordered;
    }

    private static void collectSelfWithLocalDeps(LocalProject project, Set<AppArtifactKey> addedDeps, List<LocalProject> ordered) {
        if(!project.modules.isEmpty()) {
            for(LocalProject module : project.modules) {
                collectSelfWithLocalDeps(module, addedDeps, ordered);
            }
        }
        for(Dependency dep : project.getRawModel().getDependencies()) {
            final AppArtifactKey depKey = project.getKey(dep);
            final LocalProject localDep = project.workspace.getProject(depKey);
            if(localDep == null || addedDeps.contains(depKey)) {
                continue;
            }
            collectSelfWithLocalDeps(localDep, addedDeps, ordered);
        }
        if(addedDeps.add(project.getKey())) {
            ordered.add(project);
        }
    }

    private AppArtifactKey getKey(Dependency dep) {
        return new AppArtifactKey(PROJECT_GROUPID.equals(dep.getGroupId()) ? getGroupId() : dep.getGroupId(), dep.getArtifactId());
    }
}
