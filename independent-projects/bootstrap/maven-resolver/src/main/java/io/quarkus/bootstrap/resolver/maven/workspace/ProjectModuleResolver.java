package io.quarkus.bootstrap.resolver.maven.workspace;

import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactKey;

public interface ProjectModuleResolver {

    default WorkspaceModule getProjectModule(String groupId, String artifactId) {
        return getProjectModule(ArtifactKey.ga(groupId, artifactId));
    }

    WorkspaceModule getProjectModule(ArtifactKey key);
}
