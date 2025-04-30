package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.List;
import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class UpdateJavaVersionOperation implements RewriteOperation {

    private final String newVersion;

    public UpdateJavaVersionOperation(String newVersion) {
        this.newVersion = newVersion;
    }

    @Override
    public List<Map<String, Object>> multi(BuildTool buildTool) {
        switch (buildTool) {
            case MAVEN:
                return List.of(
                        Map.of("org.openrewrite.maven.ChangePropertyValue",
                                Map.of("key", "maven.compiler.source", "newValue", newVersion)),
                        Map.of("org.openrewrite.maven.ChangePropertyValue",
                                Map.of("key", "maven.compiler.target", "newValue", newVersion)),
                        Map.of("org.openrewrite.maven.ChangePropertyValue",
                                Map.of("key", "maven.compiler.release", "newValue", newVersion)));
            case GRADLE_KOTLIN_DSL:
            case GRADLE:
                return List.of(Map.of(
                        "org.openrewrite.gradle.UpdateJavaCompatibility",
                        Map.of("version", newVersion)));
            default:
                return List.of();
        }
    }
}
