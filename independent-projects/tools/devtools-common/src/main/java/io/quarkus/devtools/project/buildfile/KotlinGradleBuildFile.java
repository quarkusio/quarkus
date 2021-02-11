package io.quarkus.devtools.project.buildfile;

import io.quarkus.devtools.project.BuildTool;

public class KotlinGradleBuildFile extends GenericGradleBuildFile {

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.GRADLE_KOTLIN_DSL;
    }

}
