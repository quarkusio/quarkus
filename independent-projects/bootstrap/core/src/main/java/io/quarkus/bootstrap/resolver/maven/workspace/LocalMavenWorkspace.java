package io.quarkus.bootstrap.resolver.maven.workspace;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;

/**
 *
 * @author Alexey Loubyansky
 */
public class LocalMavenWorkspace implements WorkspaceModelResolver, WorkspaceReader {

    private final Map<AppArtifactKey, LocalMavenProject> projects = new HashMap<>();

    private final WorkspaceRepository wsRepo = new WorkspaceRepository();
    private AppArtifactKey lastFindVersionsKey;
    private List<String> lastFindVersions;
    private long lastModified;
    private int id = 1;

    protected void addProject(LocalMavenProject project, long lastModified) {
        projects.put(project.getKey(), project);
        if(lastModified > this.lastModified) {
            this.lastModified = lastModified;
        }
        id = 31 * id + (int) (lastModified ^ (lastModified >>> 32));
    }

    public LocalMavenProject getProject(String groupId, String artifactId) {
        return getProject(new AppArtifactKey(groupId, artifactId));
    }

    public LocalMavenProject getProject(AppArtifactKey key) {
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
        final LocalMavenProject project = getProject(groupId, artifactId);
        if(project == null || !project.getVersion().equals(versionConstraint)) {
            return null;
        }
        return project.getRawModel();
    }

    @Override
    public Model resolveEffectiveModel(String groupId, String artifactId, String versionConstraint)
            throws UnresolvableModelException {
        return null;
    }

    public Map<AppArtifactKey, LocalMavenProject> getProjects() {
        return projects;
    }

    @Override
    public WorkspaceRepository getRepository() {
        return wsRepo;
    }

    @Override
    public File findArtifact(Artifact artifact) {
        final LocalMavenProject lp = getProject(artifact.getGroupId(), artifact.getArtifactId());
        if (lp == null || !lp.getVersion().equals(artifact.getVersion())) {
            return null;
        }
        final String type = artifact.getExtension();
        if (type.equals(AppArtifactCoords.TYPE_JAR)) {
            final File file = lp.getClassesDir().toFile();
            if (file.exists()) {
                return file;
            }
        } else if (type.equals(AppArtifactCoords.TYPE_POM)) {
            final File file = lp.getDir().resolve("pom.xml").toFile();
            if (file.exists()) {
                return file;
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
        final LocalMavenProject lp = getProject(lastFindVersionsKey);
        if (lp == null || !lp.getVersion().equals(artifact.getVersion())) {
            lastFindVersionsKey = null;
            return Collections.emptyList();
        }
        return lastFindVersions = Collections.singletonList(artifact.getVersion());
    }
}
