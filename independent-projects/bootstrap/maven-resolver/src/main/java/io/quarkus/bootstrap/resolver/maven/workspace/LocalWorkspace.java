package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 *
 * @author Alexey Loubyansky
 */
public class LocalWorkspace implements WorkspaceModelResolver, WorkspaceReader, ProjectModuleResolver {

    private final Map<ArtifactKey, LocalProject> projects = new HashMap<>();

    private final WorkspaceRepository wsRepo = new WorkspaceRepository();
    private ArtifactKey lastFindVersionsKey;
    private List<String> lastFindVersions;
    private long lastModified;
    private int id = 1;

    // value of the resolved version in case the raw version contains a property like ${revision} (see "Maven CI Friendly Versions")
    private String resolvedVersion;

    // added specifically to check whether empty JAR artifacts are available in the local repository
    // before creating an empty dir to represent them on the filesystem
    private BootstrapMavenContext mvnCtx;
    private LocalProject currentProject;

    protected void addProject(LocalProject project, long lastModified) {
        projects.put(project.getKey(), project);
        if (lastModified > this.lastModified) {
            this.lastModified = lastModified;
        }
        id = 31 * id + (int) (lastModified ^ (lastModified >>> 32));
    }

    public LocalProject getProject(String groupId, String artifactId) {
        return getProject(ArtifactKey.ga(groupId, artifactId));
    }

    public LocalProject getProject(ArtifactKey key) {
        return projects.get(key);
    }

    public long getLastModified() {
        return lastModified;
    }

    public int getId() {
        return id;
    }

    @Override
    public Model resolveRawModel(String groupId, String artifactId, String versionConstraint)
            throws UnresolvableModelException {
        if (findArtifact(new DefaultArtifact(groupId, artifactId, null, ArtifactCoords.TYPE_POM, versionConstraint)) != null) {
            return getProject(groupId, artifactId).getRawModel();
        }
        return null;
    }

    @Override
    public Model resolveEffectiveModel(String groupId, String artifactId, String versionConstraint)
            throws UnresolvableModelException {
        return null;
    }

    public Map<ArtifactKey, LocalProject> getProjects() {
        return projects;
    }

    @Override
    public WorkspaceRepository getRepository() {
        return wsRepo;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        final LocalProject lp = getProject(artifact.getGroupId(), artifact.getArtifactId());
        final String findVersion = artifact.getVersion();
        if (lp == null
                || !findVersion.isEmpty()
                        && !lp.getVersion().equals(findVersion)
                        && !(ModelUtils.isUnresolvedVersion(findVersion)
                                && lp.getVersion().equals(resolvedVersion))) {
            return null;
        }

        if (ArtifactCoords.TYPE_POM.equals(artifact.getExtension())) {
            final File pom = lp.getRawModel().getPomFile();
            // if the pom exists we should also check whether the main artifact can also be resolved from the workspace
            if (pom.exists() && (ArtifactCoords.TYPE_POM.equals(lp.getRawModel().getPackaging())
                    || mvnCtx != null && mvnCtx.isPreferPomsFromWorkspace()
                    || Files.exists(lp.getOutputDir())
                    || emptyJarOutput(lp, artifact) != null)) {
                return pom;
            }
        }

        // Check whether the artifact exists in the project's output dir.
        // It could also be a project with no sources/resources, in which case Maven will create an empty JAR
        // if it has previously been packaged we can use it
        Path path = lp.getOutputDir().resolve(getFileName(artifact));
        if (Files.exists(path)) {
            return path.toFile();
        }

        if (!artifact.getClassifier().isEmpty()) {
            if ("tests".equals(artifact.getClassifier())) {
                //special classifier used for test jars
                path = lp.getTestClassesDir();
                if (Files.exists(path)) {
                    return path.toFile();
                }
            }
            // otherwise, this artifact hasn't been built yet
            return null;
        }

        if (ArtifactCoords.TYPE_JAR.equals(artifact.getExtension())) {
            path = lp.getClassesDir();
            if (Files.exists(path)) {
                return path.toFile();
            }
            path = emptyJarOutput(lp, artifact);
            if (path != null) {
                return path.toFile();
            }
            // otherwise, this project hasn't been built yet
        }
        return null;
    }

    private Path emptyJarOutput(LocalProject lp, Artifact artifact) {
        // If the project has neither sources nor resources directories then it is an empty JAR.
        // If this method returns null then the Maven resolver will attempt to resolve the artifact from a repository
        // which may fail if the artifact hasn't been installed yet.
        // Here we are checking whether the artifact exists in the local repo first (Quarkus CI creates a Maven repo cache
        // first and then runs tests using '-pl' in the clean project). If the artifact exists in the local repo we return null,
        // so the Maven resolver will succeed resolving it from the repo.
        // If the artifact does not exist in the local repo, we are creating an empty classes directory in the target directory.
        if (!Files.exists(lp.getSourcesSourcesDir())
                && lp.getResourcesSourcesDirs().stream().noneMatch(Files::exists)
                && !isFoundInLocalRepo(artifact)) {
            try {
                final Path classesDir = lp.getClassesDir();
                Files.createDirectories(classesDir);
                return classesDir;
            } catch (IOException e) {
                // ignore and return null
            }
        }
        return null;
    }

    private boolean isFoundInLocalRepo(Artifact artifact) {
        final String localRepo = getLocalRepo();
        if (localRepo == null) {
            return false;
        }
        Path p = Paths.get(localRepo);
        for (String s : artifact.getGroupId().split("\\.")) {
            p = p.resolve(s);
        }
        p = p.resolve(artifact.getArtifactId());
        p = p.resolve(artifact.getVersion());
        p = p.resolve(getFileName(artifact));
        return Files.exists(p);
    }

    public static String getFileName(Artifact artifact) {
        final StringBuilder fileName = new StringBuilder();
        fileName.append(artifact.getArtifactId()).append('-').append(artifact.getVersion());
        if (!artifact.getClassifier().isEmpty()) {
            fileName.append('-').append(artifact.getClassifier());
        }
        fileName.append('.').append(artifact.getExtension());
        return fileName.toString();
    }

    private String getLocalRepo() {
        try {
            return (mvnCtx == null
                    ? mvnCtx = new BootstrapMavenContext(BootstrapMavenContext.config().setCurrentProject(currentProject))
                    : mvnCtx).getLocalRepo();
        } catch (BootstrapMavenException e) {
            return null;
        }
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        if (lastFindVersionsKey != null && lastFindVersionsKey.getArtifactId().equals(artifact.getArtifactId())
                && artifact.getVersion().equals(lastFindVersions.get(0))
                && lastFindVersionsKey.getGroupId().equals(artifact.getGroupId())) {
            return lastFindVersions;
        }
        if (findArtifact(artifact) == null) {
            return List.of();
        }
        lastFindVersionsKey = ArtifactKey.ga(artifact.getGroupId(), artifact.getArtifactId());
        return lastFindVersions = List.of(artifact.getVersion());
    }

    public String getResolvedVersion() {
        return resolvedVersion;
    }

    void setResolvedVersion(String resolvedVersion) {
        this.resolvedVersion = resolvedVersion;
    }

    LocalProject getCurrentProject() {
        return currentProject;
    }

    void setCurrentProject(LocalProject currentProject) {
        this.currentProject = currentProject;
    }

    void setBootstrapMavenContext(BootstrapMavenContext mvnCtx) {
        this.mvnCtx = mvnCtx;
    }

    @Override
    public WorkspaceModule getProjectModule(String groupId, String artifactId, String version) {
        final LocalProject project = getProject(groupId, artifactId);
        return project == null || !project.getVersion().equals(version) ? null : project.toWorkspaceModule(mvnCtx);
    }
}
