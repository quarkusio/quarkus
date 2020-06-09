package io.quarkus.bootstrap.resolver.workspace;

import io.quarkus.bootstrap.model.AppArtifactKey;
import java.util.Collection;

public interface Workspace<M extends WorkspaceProject> {

    default M getProject(String groupId, String artifactId) {
        return getProject(new AppArtifactKey(groupId, artifactId));
    }

    M getProject(AppArtifactKey key);

    Collection<M> getAllProjects();
}
