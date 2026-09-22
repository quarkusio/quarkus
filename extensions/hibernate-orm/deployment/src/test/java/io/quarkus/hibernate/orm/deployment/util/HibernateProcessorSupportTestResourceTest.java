package io.quarkus.hibernate.orm.deployment.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultSourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;

/**
 * An init script that only exists in the test resources is a likely cause of a "file not found" error
 * in dev mode or in a production build: the error message must point to it.
 */
class HibernateProcessorSupportTestResourceTest {

    @Test
    void scriptInTestResources(@TempDir Path moduleDir) throws IOException {
        Path testResources = Files.createDirectories(moduleDir.resolve("src/test/resources"));
        Path script = Files.writeString(testResources.resolve("data-test.sql"), "SELECT 1;");

        assertThat(HibernateProcessorSupport.findTestResource(module(moduleDir, testResources), "data-test.sql"))
                .isEqualTo(script.normalize());
    }

    @Test
    void scriptInTestResourcesSubdirectory(@TempDir Path moduleDir) throws IOException {
        Path testResources = Files.createDirectories(moduleDir.resolve("src/test/resources"));
        Files.createDirectories(testResources.resolve("sql"));
        Path script = Files.writeString(testResources.resolve("sql/data-test.sql"), "SELECT 1;");

        assertThat(HibernateProcessorSupport.findTestResource(module(moduleDir, testResources), "sql/data-test.sql"))
                .isEqualTo(script.normalize());
    }

    @Test
    void scriptNotInTestResources(@TempDir Path moduleDir) throws IOException {
        Path testResources = Files.createDirectories(moduleDir.resolve("src/test/resources"));

        assertThat(HibernateProcessorSupport.findTestResource(module(moduleDir, testResources), "data-test.sql"))
                .isNull();
    }

    @Test
    void scriptOutsideTestResources(@TempDir Path moduleDir) throws IOException {
        Path testResources = Files.createDirectories(moduleDir.resolve("src/test/resources"));
        Path elsewhere = Files.writeString(moduleDir.resolve("data-test.sql"), "SELECT 1;");

        assertThat(HibernateProcessorSupport.findTestResource(module(moduleDir, testResources), "../../../data-test.sql"))
                .as("a path escaping the test resources is not reported as a test resource")
                .isNull();
        assertThat(HibernateProcessorSupport.findTestResource(module(moduleDir, testResources),
                elsewhere.toAbsolutePath().toString()))
                .as("an absolute path is not reported as a test resource")
                .isNull();
    }

    @Test
    void noApplicationModule() {
        assertThat(HibernateProcessorSupport.findTestResource(null, "data-test.sql")).isNull();
    }

    @Test
    void noTestSources(@TempDir Path moduleDir) {
        WorkspaceModule module = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "app", "1.0"))
                .setModuleDir(moduleDir)
                .setBuildDir(moduleDir.resolve("target"))
                .build();

        assertThat(HibernateProcessorSupport.findTestResource(module, "data-test.sql")).isNull();
    }

    private static WorkspaceModule module(Path moduleDir, Path testResources) {
        return WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "app", "1.0"))
                .setModuleDir(moduleDir)
                .setBuildDir(moduleDir.resolve("target"))
                .addArtifactSources(new DefaultArtifactSources(ArtifactSources.TEST, List.of(),
                        List.of(new DefaultSourceDir(testResources, moduleDir.resolve("target/test-classes"), null))))
                .build();
    }
}
