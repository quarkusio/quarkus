package io.quarkus.devtools.project.update.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.RewriteOperation;

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
    public Map<String, Object> toMap(BuildTool buildTool) {
        switch (buildTool) {
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
