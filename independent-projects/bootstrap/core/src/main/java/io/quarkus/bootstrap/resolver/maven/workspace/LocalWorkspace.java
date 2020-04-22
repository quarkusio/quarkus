package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.maven.model.Model;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 *
 * @author Alexey Loubyansky
 */
public class LocalWorkspace implements WorkspaceModelResolver, WorkspaceReader {

    private final Map<AppArtifactKey, LocalProject> projects = new HashMap<>();

    private final WorkspaceRepository wsRepo = new WorkspaceRepository();
    private AppArtifactKey lastFindVersionsKey;
    private List<String> lastFindVersions;
    private long lastModified;
    private int id = 1;

    // value of the resolved version in case the raw version contains a property like ${revision} (see "Maven CI Friendly Versions")
    private String resolvedVersion;

    protected void addProject(LocalProject project, long lastModified) {
        projects.put(project.getKey(), project);
        if (lastModified > this.lastModified) {
            this.lastModified = lastModified;
        }
        id = 31 * id + (int) (lastModified ^ (lastModified >>> 32));
    }

    public LocalProject getProject(String groupId, String artifactId) {
        return getProject(new AppArtifactKey(groupId, artifactId));
    }

    public LocalProject getProject(AppArtifactKey key) {
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
        final LocalProject project = getProject(groupId, artifactId);
        if (project == null || !project.getVersion().equals(versionConstraint)) {
            return null;
        }
        return project.getRawModel();
    }

    @Override
    public Model resolveEffectiveModel(String groupId, String artifactId, String versionConstraint)
            throws UnresolvableModelException {
        return null;
    }

    public Map<AppArtifactKey, LocalProject> getProjects() {
        return projects;
    }

    @Override
    public WorkspaceRepository getRepository() {
        return wsRepo;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        final LocalProject lp = getProject(artifact.getGroupId(), artifact.getArtifactId());
        if (lp == null
                || !lp.getVersion().equals(artifact.getVersion())
                        && !(LocalProject.isUnresolvedVersion(artifact.getVersion())
                                && lp.getVersion().equals(resolvedVersion))) {
            return null;
        }
        if (!Objects.equals(artifact.getClassifier(), lp.getAppArtifact().getClassifier())) {
            if ("tests".equals(artifact.getClassifier())) {
                //special classifier used for test jars
                final Path path = lp.getTestClassesDir();
                if (Files.exists(path)) {
                    return path.toFile();
                }
            }
            return null;
        }
        final String type = artifact.getExtension();
        if (type.equals(AppArtifactCoords.TYPE_JAR)) {
            Path path = lp.getClassesDir();
            if (Files.exists(path)) {
                return path.toFile();
            }

            // it could be a project with no sources/resources, in which case Maven will create an empty JAR
            // if it has previously been packaged we can return it
            path = lp.getOutputDir().resolve(lp.getArtifactId() + "-" + lp.getVersion() + ".jar");
            if (Files.exists(path)) {
                return path.toFile();
            }
        } else if (type.equals(AppArtifactCoords.TYPE_POM)) {
            final Path path = lp.getDir().resolve("pom.xml");
            if (Files.exists(path)) {
                return path.toFile();
            }
        }
        return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        if (lastFindVersionsKey != null && artifact.getVersion().equals(lastFindVersions.get(0))
                && lastFindVersionsKey.getArtifactId().equals(artifact.getArtifactId())
                && lastFindVersionsKey.getGroupId().equals(artifact.getGroupId())) {
            return lastFindVersions;
        }
        lastFindVersionsKey = new AppArtifactKey(artifact.getGroupId(), artifact.getArtifactId());
        final LocalProject lp = getProject(lastFindVersionsKey);
        if (lp == null || !lp.getVersion().equals(artifact.getVersion())) {
            lastFindVersionsKey = null;
            return Collections.emptyList();
        }
        return lastFindVersions = Collections.singletonList(artifact.getVersion());
    }

    public String getResolvedVersion() {
        return resolvedVersion;
    }

    void setResolvedVersion(String resolvedVersion) {
        this.resolvedVersion = resolvedVersion;
    }
}
