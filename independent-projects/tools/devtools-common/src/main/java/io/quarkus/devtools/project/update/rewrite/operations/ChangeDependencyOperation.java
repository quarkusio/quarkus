package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class ChangeDependencyOperation implements RewriteOperation {

    private final String oldGroupId;
    private final String oldArtifactId;
    private final String newGroupId;
    private final String newArtifactId;
    private final String newVersion;

    public ChangeDependencyOperation(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId,
            String newVersion) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
    }

    @Override
    public Map<String, Object> single(BuildTool buildTool) {
        switch (buildTool) {
            case GRADLE_KOTLIN_DSL:
            case GRADLE:
            case MAVEN:
                return Map.of("org.openrewrite.java.dependencies.ChangeDependency",
                        Map.of(
                                "oldGroupId", oldGroupId,
                                "oldArtifactId", oldArtifactId,
                                "newGroupId", newGroupId,
                                "newArtifactId", newArtifactId,
                                "newVersion", newVersion));
            default:
                return Map.of();
        }
    }

}
