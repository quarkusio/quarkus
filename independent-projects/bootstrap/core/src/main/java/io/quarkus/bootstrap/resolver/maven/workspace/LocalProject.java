package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.model.Dependency;
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

    /**
     * Matches specific properties that are allowed to be used in a version as per Maven spec.
     *
     * @see <a href="https://maven.apache.org/maven-ci-friendly.html">Maven CI Friendly Versions (maven.apache.org)</a>
     */
    private static final Pattern UNRESOLVED_VERSION_PATTERN = Pattern
            .compile(Pattern.quote("${") + "(revision|sha1|changelist)" + Pattern.quote("}"));

    public static LocalProject load(Path path) throws BootstrapException {
        return load(path, true);
    }

    public static LocalProject load(Path path, boolean required) throws BootstrapException {
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

    public static LocalProject loadWorkspace(Path path) throws BootstrapException {
        return loadWorkspace(path, true);
    }

    public static LocalProject loadWorkspace(Path path, boolean required) throws BootstrapException {
        path = path.normalize().toAbsolutePath();
        Path currentProjectPom = null;
        Model rootModel = null;
        if (!Files.isDirectory(path)) {
            // see if that's an actual pom
            try {
                rootModel = loadRootModel(path);
                if (rootModel != null) {
                    currentProjectPom = path;
                }
            } catch (BootstrapException e) {
                // ignore, it's not a POM file, we'll be looking for the POM later
            }
        }
        if (currentProjectPom == null) {
            currentProjectPom = locateCurrentProjectPom(path, required);
            if (currentProjectPom == null) {
                return null;
            }
            rootModel = loadRootModel(currentProjectPom);
        }
        return loadWorkspace(currentProjectPom, rootModel);
    }

    /**
     * Loads the workspace the current project belongs to.
     * If current project does not exist then the method will return null.
     *
     * @param ctx bootstrap maven context
     * @return current project with the workspace or null in case the current project could not be resolved
     * @throws BootstrapException in case of an error
     */
    public static LocalProject loadWorkspace(BootstrapMavenContext ctx) throws BootstrapException {
        final Path currentProjectPom = ctx.getCurrentProjectPomOrNull();
        if (currentProjectPom == null) {
            return null;
        }
        final Path rootProjectBaseDir = ctx.getRootProjectBaseDir();
        final Model rootModel = rootProjectBaseDir == null || rootProjectBaseDir.equals(currentProjectPom.getParent())
                ? loadRootModel(currentProjectPom)
                : readModel(rootProjectBaseDir.resolve(POM_XML));
        return loadWorkspace(currentProjectPom, rootModel);
    }

    private static LocalProject loadWorkspace(Path currentProjectPom, Model rootModel) throws BootstrapException {
        final LocalWorkspace ws = new LocalWorkspace();
        final LocalProject project = load(ws, null, rootModel, currentProjectPom.getParent());
        return project == null ? load(ws, null, readModel(currentProjectPom), currentProjectPom.getParent()) : project;
    }

    private static LocalProject load(LocalWorkspace workspace, LocalProject parent, Model model, Path currentProjectDir)
            throws BootstrapException {
        final LocalProject project = new LocalProject(model, workspace);
        if (parent != null) {
            parent.modules.add(project);
        }
        LocalProject result = currentProjectDir == null || !currentProjectDir.equals(project.getDir()) ? null : project;
        final List<String> modules = project.getRawModel().getModules();
        if (!modules.isEmpty()) {
            Path dirArg = result == null ? currentProjectDir : null;
            for (String module : modules) {
                final LocalProject loaded = load(workspace, project,
                        readModel(project.getDir().resolve(module).resolve(POM_XML)), dirArg);
                if (loaded != null && result == null) {
                    result = loaded;
                    dirArg = null;
                }
            }
        }
        return result;
    }

    private static Model loadRootModel(Path pomXml) throws BootstrapException {
        Model model = null;
        while (Files.exists(pomXml)) {
            model = readModel(pomXml);
            final Parent parent = model.getParent();
            if (parent != null
                    && parent.getRelativePath() != null
                    && !parent.getRelativePath().isEmpty()) {
                pomXml = pomXml.getParent().resolve(parent.getRelativePath()).normalize();
                if (Files.isDirectory(pomXml)) {
                    pomXml = pomXml.resolve(POM_XML);
                }
            } else {
                pomXml = pomXml.getParent().getParent().resolve(POM_XML);
            }
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

    private static Path locateCurrentProjectPom(Path path, boolean required) throws BootstrapException {
        Path p = path;
        while (p != null) {
            final Path pom = p.resolve(POM_XML);
            if (Files.exists(pom)) {
                return pom;
            }
            p = p.getParent();
        }
        if (required) {
            throw new BootstrapException("Failed to locate project pom.xml for " + path);
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

    private LocalProject(Model rawModel, LocalWorkspace workspace) throws BootstrapException {
        this.rawModel = rawModel;
        this.dir = rawModel.getProjectDirectory().toPath();
        this.workspace = workspace;
        this.groupId = ModelUtils.getGroupId(rawModel);
        this.artifactId = rawModel.getArtifactId();

        final String rawVersion = ModelUtils.getVersion(rawModel);
        final boolean rawVersionIsUnresolved = isUnresolvedVersion(rawVersion);
        String resolvedVersion = rawVersionIsUnresolved ? resolveVersion(rawVersion, rawModel) : rawVersion;

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

    static boolean isUnresolvedVersion(String version) {
        return UNRESOLVED_VERSION_PATTERN.matcher(version).find();
    }

    private static String resolveVersion(String rawVersion, Model rawModel) {
        final Map<String, String> props = new HashMap<>();
        rawModel.getProperties().entrySet().forEach(e -> props.put(e.getKey().toString(), e.getValue().toString()));
        System.getProperties().entrySet().forEach(e -> props.put(e.getKey().toString(), e.getValue().toString()));

        Matcher matcher = UNRESOLVED_VERSION_PATTERN.matcher(rawVersion);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            final String resolved = props.get(matcher.group(1));
            if (resolved == null) {
                return null;
            }
            matcher.appendReplacement(sb, resolved);
        }
        matcher.appendTail(sb);
        return sb.toString();
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
        return new AppArtifact(groupId, artifactId, BootstrapConstants.EMPTY, extension, version);
    }

    public List<LocalProject> getSelfWithLocalDeps() {
        if (workspace == null) {
            return Collections.singletonList(this);
        }
        final List<LocalProject> ordered = new ArrayList<>();
        collectSelfWithLocalDeps(this, new HashSet<>(), ordered);
        return ordered;
    }

    private static void collectSelfWithLocalDeps(LocalProject project, Set<AppArtifactKey> addedDeps,
            List<LocalProject> ordered) {
        if (!project.modules.isEmpty()) {
            for (LocalProject module : project.modules) {
                collectSelfWithLocalDeps(module, addedDeps, ordered);
            }
        }
        for (Dependency dep : project.getRawModel().getDependencies()) {
            final AppArtifactKey depKey = project.getKey(dep);
            final LocalProject localDep = project.workspace.getProject(depKey);
            if (localDep == null || addedDeps.contains(depKey)) {
                continue;
            }
            collectSelfWithLocalDeps(localDep, addedDeps, ordered);
        }
        if (addedDeps.add(project.getKey())) {
            ordered.add(project);
        }
    }

    private AppArtifactKey getKey(Dependency dep) {
        return new AppArtifactKey(PROJECT_GROUPID.equals(dep.getGroupId()) ? getGroupId() : dep.getGroupId(),
                dep.getArtifactId());
    }

    private Path resolveRelativeToBaseDir(String path, String defaultPath) {
        return dir.resolve(path == null ? defaultPath : stripProjectBasedirPrefix(path));
    }

    private static String stripProjectBasedirPrefix(String path) {
        return path.startsWith(PROJECT_BASEDIR) ? path.substring(PROJECT_BASEDIR.length() + 1) : path;
    }
}
