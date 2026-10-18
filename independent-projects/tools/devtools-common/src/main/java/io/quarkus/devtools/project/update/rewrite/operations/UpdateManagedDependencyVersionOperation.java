package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class UpdateManagedDependencyVersionOperation implements RewriteOperation {

    private final String groupId;
    private final String artifactId;
    private final String newVersion;

    public UpdateManagedDependencyVersionOperation(String groupId, String artifactId, String newVersion) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.newVersion = newVersion;
    }

    @Override
    public Map<String, Object> single(BuildTool buildTool) {
        switch (buildTool) {
            case MAVEN:
                return Map.of("org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId",
                        Map.of(
                                "oldGroupId", groupId,
                                "oldArtifactId", artifactId,
                                "newGroupId", groupId,
                                "newArtifactId", artifactId,
                                "newVersion", newVersion));
            default:
                return Map.of();
        }
    }

}
