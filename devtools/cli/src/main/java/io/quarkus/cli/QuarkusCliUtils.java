package io.quarkus.cli;

import java.nio.file.Path;

import io.quarkus.cli.core.QuarkusCliVersion;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.buildfile.MavenProjectBuildFile;

public class QuarkusCliUtils {

    static QuarkusProject getQuarkusProject(Path projectRoot) {
        return getQuarkusProject(QuarkusProject.resolveExistingProjectBuildTool(projectRoot), projectRoot);
    }

    static QuarkusProject getQuarkusProject(BuildTool buildTool, Path projectRoot) {
        if (BuildTool.MAVEN.equals(buildTool)) {
            return MavenProjectBuildFile.getProject(projectRoot.toAbsolutePath(), MessageWriter.info(),
                    () -> QuarkusCliVersion.version());
        }
        if (BuildTool.GRADLE.equals(buildTool)) {
            return getNonMavenProject(projectRoot, BuildTool.GRADLE);
        }
        if (BuildTool.GRADLE_KOTLIN_DSL.equals(buildTool)) {
            return getNonMavenProject(projectRoot, BuildTool.GRADLE_KOTLIN_DSL);
        }
        throw new IllegalArgumentException("Unexpected build tool " + buildTool);
    }

    private static QuarkusProject getNonMavenProject(Path projectRoot, BuildTool buildTool) {
        return QuarkusProjectHelper.getProject(projectRoot, buildTool, null);
    }
}
