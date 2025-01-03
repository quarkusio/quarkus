package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class AddManagedDependencyOperation implements RewriteOperation {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String type;
    private final String scope;

    public AddManagedDependencyOperation(String groupId, String artifactId, String version, String type, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.scope = scope;
    }

    @Override
    public Map<String, Object> single(BuildTool buildTool) {
        switch (buildTool) {
            case GRADLE_KOTLIN_DSL:
            case GRADLE:
                throw new IllegalStateException("AddManagedDependencyOperation is not supported for Gradle at the moment");
            case MAVEN:
                return Map.of("org.openrewrite.maven.AddManagedDependency",
                        Map.of(
                                "groupId", groupId,
                                "artifactId", artifactId,
                                "version", version,
                                "type", type,
                                "scope", scope));
            default:
                return Map.of();
        }
    }

}
