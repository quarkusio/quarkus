package io.quarkus.cli.create;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.quickcli.annotations.Option;

public class TargetBuildToolGroup {
    @Option(names = { "--jbang" }, description = "Use JBang (Java only)")
    boolean jbang = false;

    @Option(names = { "--maven" }, description = "Use Maven")
    boolean maven = false;

    @Option(names = { "--gradle" }, description = "Use Gradle")
    boolean gradle = false;

    @Option(names = { "--gradle-kotlin-dsl" }, description = "Use Gradle with Kotlin DSL")
    boolean gradleKotlinDsl = false;

    public boolean isBuildless() {
        return jbang;
    }

    public BuildTool getBuildTool(BuildTool defaultTool) {
        if (gradleKotlinDsl) {
            return BuildTool.GRADLE_KOTLIN_DSL;
        }
        if (gradle) {
            return BuildTool.GRADLE;
        }
        if (jbang) {
            return BuildTool.JBANG;
        }
        if (maven) {
            return BuildTool.MAVEN;
        }
        return defaultTool;
    }

    @Override
    public String toString() {
        return "TargetBuildToolGroup [gradle=" + gradle + ", gradleKotlinDsl=" + gradleKotlinDsl + ", jbang=" + jbang
                + ", maven=" + maven + "]";
    }
}
