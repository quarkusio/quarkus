package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class UpdateDependencyVersionOperation implements RewriteOperation {

    private final String groupId;
    private final String artifactId;
    private final String newVersion;

    public UpdateDependencyVersionOperation(String groupId, String artifactId, String newVersion) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.newVersion = newVersion;
    }

    @Override
    public Map<String, Object> single(BuildTool buildTool) {
        switch (buildTool) {
            case GRADLE_KOTLIN_DSL:
            case GRADLE:
                return Map.of("org.openrewrite.gradle.UpgradeDependencyVersion",
                        Map.of(
                                "groupId", groupId,
                                "artifactId", artifactId,
                                "newVersion", newVersion));
            case MAVEN:
                return Map.of("org.openrewrite.maven.UpgradeDependencyVersion",
                        Map.of(
                                "groupId", groupId,
                                "artifactId", artifactId,
                                "newVersion", newVersion));
            default:
                return Map.of();
        }
    }

}
