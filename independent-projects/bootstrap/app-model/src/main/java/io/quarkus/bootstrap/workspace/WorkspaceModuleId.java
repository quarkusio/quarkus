package io.quarkus.bootstrap.workspace;

import io.quarkus.maven.dependency.GAV;

public interface WorkspaceModuleId {

    static WorkspaceModuleId of(String groupId, String artifactId, String version) {
        return new GAV(groupId, artifactId, version);
    }

    String getGroupId();

    String getArtifactId();

    String getVersion();
}
