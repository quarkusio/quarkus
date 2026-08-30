package io.quarkus.gradle.application.internal.execution.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.execution.BuildRequest;
import io.quarkus.gradle.application.internal.execution.ImageOperation;
import io.quarkus.gradle.application.internal.execution.ImageRequest;
import io.quarkus.gradle.application.internal.image.ContainerImageTarget;
import io.quarkus.gradle.application.internal.planning.OutputLayout;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

class WorkerBackedBuildOperationsTest {

    @TempDir
    Path directory;

    @Test
    void mapsRequestToWorkerSubmissionWithoutMixingBootstrapAndForkedSystemProperties() throws Exception {
        Path model = directory.resolve("app-model.dat");
        Files.createDirectories(directory.resolve("app"));
        ToolingUtils.serializeAppModel(new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("io.acme")
                        .setArtifactId("app")
                        .setVersion("1.0")
                        .setType("jar")
                        .setResolvedPath(directory.resolve("app/app.jar")))
                .setPlatformImports(PlatformImports.fromMap(Map.of()))
                .build(), model);
        var project = ProjectBuilder.builder().build();
        var operations = new WorkerBackedBuildOperations(null, project.getProviders(),
                new ForkOptionsSnapshot(List.of(), Map.of(), Map.of(), null, null, false, false, null), null, null);

        var request = new BuildRequest(
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR),
                directory.resolve("quarkus-builds/app/package"),
                model,
                List.of(),
                Set.of(),
                new EffectiveConfigPlan(
                        Map.of("full", "value"),
                        Map.of("quarkus.worker", "worker-value"),
                        Map.of("quarkus.package.output-name", "mapped-app", "build.system", "bootstrap-value"),
                        Map.of()),
                Map.of("quarkus.package.output-name", "mapped-app", "build.system", "bootstrap-value"),
                Map.of(),
                true,
                new OutputLayout(directory.resolve("root"), directory.resolve("gen"),
                        directory.resolve("app"), Optional.empty()));

        var submission = operations.workerBuildSubmission(request, Optional.of(directory.resolve("augment.properties")));

        assertThat(submission.buildSystemProperties())
                .containsEntry("build.system", "bootstrap-value")
                .doesNotContainKey("quarkus.worker");
        assertThat(submission.forkedSystemProperties())
                .containsEntry("quarkus.worker", "worker-value")
                .doesNotContainKey("build.system");
        assertThat(submission.baseName()).isEqualTo("mapped-app");
        assertThat(submission.targetDirectory()).isEqualTo(directory.resolve("quarkus-builds/app/package"));
        assertThat(submission.augmentResultFile()).contains(directory.resolve("augment.properties"));
        assertThat(submission.appModel().getAppArtifact().getGroupId()).isEqualTo("io.acme");
        assertThat(submission.processIsolated()).isTrue();
        assertThat(submission.gradleVersion()).isNotBlank();
    }

    @Test
    void missingContainerImageResultMessageReportsObservedResultTypesAndBuilderHint() {
        var request = new ImageRequest(
                buildRequest(),
                ImageOperation.BUILD,
                Optional.of(new ContainerImageTarget("quay.io/acme/app:1.0")),
                Optional.empty(),
                Map.of(),
                Map.of(),
                directory.resolve("receipt.properties"),
                Optional.empty(),
                Optional.empty());

        assertThat(WorkerBackedBuildOperations.missingContainerImageResultMessage(request, List.of(
                new ArtifactResult(Path.of("build/quarkus-run.jar"), "jar", Map.of()),
                new ArtifactResult(Path.of("build/app-sources.jar"), "sources", Map.of()),
                new ArtifactResult(null, null, Map.of()))))
                .contains("Quarkus image operation for 'app' did not produce a container image result")
                .contains("Observed augmentation result types: <missing>, jar, sources")
                .contains("quarkus-container-image-jib")
                .contains("image.builder")
                .contains("quarkus.container-image.builder");
    }

    private BuildRequest buildRequest() {
        return new BuildRequest(
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR),
                directory.resolve("quarkus-builds/app/package"),
                directory.resolve("app-model.dat"),
                List.of(),
                Set.of(),
                new EffectiveConfigPlan(Map.of(), Map.of(), Map.of(), Map.of()),
                Map.of(),
                Map.of(),
                true,
                new OutputLayout(directory.resolve("root"), directory.resolve("gen"),
                        directory.resolve("app"), Optional.empty()));
    }
}
