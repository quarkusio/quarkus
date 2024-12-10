package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class RemoveManagedDependencyOperation implements RewriteOperation {

    private final String groupId;
    private final String artifactId;

    public RemoveManagedDependencyOperation(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    @Override
    public Map<String, Object> single(BuildTool buildTool) {
        switch (buildTool) {
            case GRADLE_KOTLIN_DSL:
            case GRADLE:
                return Map.of("org.openrewrite.gradle.RemoveDependency",
                        Map.of(
                                "groupId", groupId,
                                "artifactId", artifactId));
            case MAVEN:
                return Map.of("org.openrewrite.maven.RemoveManagedDependency",
                        Map.of(
                                "groupId", groupId,
                                "artifactId", artifactId));
            default:
                return Map.of();
        }
    }

}
