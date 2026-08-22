package io.quarkus.gradle.application.internal.codegen.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.gradle.application.internal.codegen.CodegenRequest;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.execution.worker.ForkOptionsSnapshot;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.runtime.LaunchMode;

class WorkerBackedCodegenOperationsTest {

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
        var operations = new WorkerBackedCodegenOperations(null, project.getProviders(),
                new ForkOptionsSnapshot(List.of(), Map.of(), Map.of(), null, null, false, false, null), null, null);

        var request = new CodegenRequest(
                model,
                LaunchMode.NORMAL,
                Set.of(directory.resolve("src/main").toFile()),
                directory.resolve("generated"),
                directory.resolve("build"),
                "app",
                List.of("grpc"),
                List.of("proto"),
                List.of(directory.resolve("app.jar")),
                new EffectiveConfigPlan(
                        Map.of("full", "value"),
                        Map.of("quarkus.worker", "worker-value"),
                        Map.of("quarkus.package.output-name", "mapped-app", "build.system", "bootstrap-value"),
                        Map.of()),
                Map.of("quarkus.package.output-name", "mapped-app", "build.system", "bootstrap-value"));

        var submission = operations.workerCodegenSubmission(request);

        assertThat(submission.buildSystemProperties())
                .containsEntry("build.system", "bootstrap-value")
                .doesNotContainKey("quarkus.worker");
        assertThat(submission.forkedSystemProperties())
                .containsEntry("quarkus.worker", "worker-value")
                .doesNotContainKey("build.system");
        assertThat(submission.baseName()).isEqualTo("mapped-app");
        assertThat(submission.targetDirectory()).isEqualTo(directory.resolve("build"));
        assertThat(submission.appModel().getAppArtifact().getGroupId()).isEqualTo("io.acme");
        assertThat(submission.processIsolated()).isTrue();
        assertThat(submission.gradleVersion()).isNotBlank();
    }
}
