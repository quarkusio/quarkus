package io.quarkus.bootstrap.workspace;

import io.quarkus.maven.dependency.GAV;

public interface WorkspaceModuleId {

    static WorkspaceModuleId of(String groupId, String artifactId, String version) {
        return new GAV(groupId, artifactId, version);
    }

    static WorkspaceModuleId fromString(String str) {
        final String[] arr = str.split(":");
        if (arr.length != 3) {
            throw new IllegalArgumentException("Invalid workspace module ID string: " + str);
        }
        return of(arr[0], arr[1], arr[2]);
    }

    String getGroupId();

    String getArtifactId();

    String getVersion();
}
