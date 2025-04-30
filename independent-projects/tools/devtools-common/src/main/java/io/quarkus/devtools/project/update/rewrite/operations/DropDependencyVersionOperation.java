package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class DropDependencyVersionOperation implements RewriteOperation {

    private final String groupId;
    private final String artifactId;

    public DropDependencyVersionOperation(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    @Override
    public Map<String, Object> single(BuildTool buildTool) {
        switch (buildTool) {
            // Gradle is not yet implemented: https://github.com/openrewrite/rewrite/issues/3546
            case MAVEN:
                return Map.of("org.openrewrite.maven.RemoveRedundantDependencyVersions",
                        Map.of(
                                "groupId", groupId,
                                "artifactId", artifactId, "onlyIfManagedVersionIs", "ANY"));
            default:
                return Map.of();
        }
    }

}
