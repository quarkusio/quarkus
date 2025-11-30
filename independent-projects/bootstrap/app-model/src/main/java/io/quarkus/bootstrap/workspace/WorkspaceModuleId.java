package io.quarkus.bootstrap.workspace;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.quarkus.maven.dependency.GAV;

public interface WorkspaceModuleId {

    @JsonCreator
    static WorkspaceModuleId of(String groupId, String artifactId, String version) {
        return new GAV(groupId, artifactId, version);
    }

    String getGroupId();

    String getArtifactId();

    String getVersion();
}
