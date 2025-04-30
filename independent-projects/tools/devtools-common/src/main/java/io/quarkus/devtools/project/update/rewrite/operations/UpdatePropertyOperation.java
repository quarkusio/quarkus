package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class UpdatePropertyOperation implements RewriteOperation {

    private final String key;
    private final String newValue;

    public UpdatePropertyOperation(String key, String newValue) {
        this.key = key;
        this.newValue = newValue;
    }

    @Override
    public Map<String, Object> single(BuildTool buildTool) {
        switch (buildTool) {
            case MAVEN:
                return Map.of("org.openrewrite.maven.ChangePropertyValue",
                        Map.of("key", key, "newValue", newValue));
            case GRADLE:
            case GRADLE_KOTLIN_DSL:
                return Map.of(
                        "org.openrewrite.gradle.AddProperty",
                        Map.of("key", key, "value", newValue, "overwrite", true));
            default:
                return Map.of();
        }
    }
}
