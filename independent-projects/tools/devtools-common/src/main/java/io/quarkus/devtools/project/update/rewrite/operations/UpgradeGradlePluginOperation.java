package io.quarkus.devtools.project.update.rewrite.operations;

import java.util.Map;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.RewriteOperation;

public class UpgradeGradlePluginOperation implements RewriteOperation {

    private final String pluginIdPattern;
    private final String newVersion;

    public UpgradeGradlePluginOperation(String pluginIdPattern, String newVersion) {
        this.pluginIdPattern = pluginIdPattern;
        this.newVersion = newVersion;
    }

    @Override
    public Map<String, Object> single(BuildTool buildTool) {
        switch (buildTool) {
            case GRADLE_KOTLIN_DSL:
            case GRADLE:
                return Map.of(
                        "org.openrewrite.gradle.plugins.UpgradePluginVersion",
                        Map.of("pluginIdPattern", pluginIdPattern, "newVersion", newVersion));
            default:
                throw new UnsupportedOperationException("This operation is only supported for Gradle projects");
        }
    }
}
