package io.quarkus.devtools.project.buildfile;

import io.quarkus.devtools.project.BuildTool;

public class GroovyGradleBuildFile extends GenericGradleBuildFile {

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.GRADLE;
    }

}
