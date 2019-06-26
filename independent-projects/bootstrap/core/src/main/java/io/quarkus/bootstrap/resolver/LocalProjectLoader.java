package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.resolver.gradle.LocalGradleProject;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalMavenProject;

public class LocalProjectLoader {

    private static boolean isGradleProjectDirectory(Path p) {
        return Files.exists(p.resolve("build.gradle")) || Files.exists(p.resolve("build.gradle.kts"));
    }

    private static boolean isMavenProjectDirectory(Path p) {
        return Files.exists(p.resolve("pom.xml"));
    }

    public LocalProject open(Path appOutputDir) throws BootstrapException {
        Path projectDir = appOutputDir;
        while (projectDir != null) {
            if (isMavenProjectDirectory(projectDir)) {
                return LocalMavenProject.load(projectDir);
            } else if (isGradleProjectDirectory(projectDir)) {
                return LocalGradleProject.load(projectDir);
            }

            projectDir = projectDir.getParent();
        }

        throw new BootstrapException("Project not found in " + appOutputDir);
    }
}
