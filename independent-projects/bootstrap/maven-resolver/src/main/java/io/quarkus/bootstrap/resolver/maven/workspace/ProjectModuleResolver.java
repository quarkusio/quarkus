package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactKey;

public interface ProjectModuleResolver {

    WorkspaceModule getProjectModule(String groupId, String artifactId);

    default WorkspaceModule getProjectModule(ArtifactKey key) {
        return getProjectModule(key.getGroupId(), key.getArtifactId());
    }
}
