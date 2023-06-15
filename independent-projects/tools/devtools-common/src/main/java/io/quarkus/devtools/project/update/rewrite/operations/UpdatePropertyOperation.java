package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class UpdatePropertyOperation implements RewriteOperation {

    public String key;
    public String newValue;

    public UpdatePropertyOperation(String key, String newValue) {
        this.key = key;
        this.newValue = newValue;
    }

    @Override
    public Map<String, Object> toMap(BuildTool buildTool) {
        switch (buildTool) {
            case MAVEN:
                return Map.of("org.openrewrite.maven.ChangePropertyValue",
                        Map.of("key", key, "newValue", newValue));
            case GRADLE:
                return Map.of(
                        "org.openrewrite.gradle.AddProperty",
                        Map.of("key", key, "value", newValue, "overwrite", true));
            default:
                return Map.of();
        }
    }
}
