package io.quarkus.gradle.application;

import java.io.IOException;
import java.nio.file.Path;

import io.quarkus.gradle.testing.BaseGradleTest;

abstract class ContinuousTestingParityProject {

    private final Path projectDirectory;

    ContinuousTestingParityProject(Path projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    final void writeFile(String relativePath, String content) throws IOException {
        BaseGradleTest.writeFile(projectDirectory.resolve(relativePath), content);
    }

    final String propertyPath(String relativePath) {
        return projectDirectory.resolve(relativePath).toAbsolutePath().toString().replace('\\', '/');
    }
}
