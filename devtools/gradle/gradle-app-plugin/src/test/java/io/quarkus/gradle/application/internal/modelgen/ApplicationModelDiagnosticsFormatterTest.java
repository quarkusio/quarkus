package io.quarkus.gradle.application.internal.modelgen;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

class ApplicationModelDiagnosticsFormatterTest {

    @TempDir
    Path projectDirectory;

    @Test
    void formatsDeterministicDependencyAndWorkspaceModuleDetails() {
        Path sourceDirectory = projectDirectory.resolve("src/main/java");
        Path classesDirectory = projectDirectory.resolve("build/classes/java/main");
        WorkspaceModule.Mutable applicationModule = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "app", "1.0"))
                .setModuleDir(projectDirectory)
                .setBuildDir(projectDirectory.resolve("build"))
                .setBuildFile(projectDirectory.resolve("build.gradle"))
                .addDependency(new ArtifactDependency(ArtifactCoords.jar("org.acme", "library", "1.0")))
                .addArtifactSources(new DefaultArtifactSources(ArtifactSources.MAIN,
                        List.of(SourceDir.of(sourceDirectory, classesDirectory)), List.of()));

        ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setCoords(ArtifactCoords.jar("org.acme", "app", "1.0"))
                .setResolvedPath(classesDirectory)
                .setWorkspaceModule(applicationModule)
                .setRuntimeCp()
                .setDeploymentCp()
                .setReloadable();
        ResolvedDependencyBuilder library = ResolvedDependencyBuilder.newInstance()
                .setCoords(ArtifactCoords.jar("org.acme", "library", "1.0"))
                .setResolvedPath(projectDirectory.resolve("../library/build/libs/library-1.0.jar"))
                .setRuntimeCp()
                .setDeploymentCp()
                .setDirect(true);
        ApplicationModel model = new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .addDependency(library)
                .addReloadableWorkspaceModule(appArtifact.getKey())
                .build();

        String diagnostics = ApplicationModelDiagnosticsFormatter.format(
                "development", model, projectDirectory, projectDirectory);

        assertThat(diagnostics)
                .startsWith("Quarkus application model 'development':")
                .contains("org.acme:app::jar:1.0")
                .contains("flags=runtime-cp, deployment-cp, workspace-module, reloadable")
                .contains("<project>/build/classes/java/main")
                .contains("org.acme:library::jar:1.0")
                .contains("flags=direct, runtime-cp, deployment-cp")
                .contains("workspace-module=<none>")
                .contains("module-directory=<project>")
                .contains("directory=<project>/src/main/java output=<project>/build/classes/java/main")
                .contains("Reloadable workspace dependencies:\n    org.acme:app::jar")
                .endsWith("Imported platform BOMs:\n    <none>\n"
                        + "Platform property values are omitted from this diagnostics report.");
    }
}
