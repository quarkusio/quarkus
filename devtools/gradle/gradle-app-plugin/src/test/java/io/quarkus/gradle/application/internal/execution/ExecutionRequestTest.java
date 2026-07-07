package io.quarkus.gradle.application.internal.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.image.ContainerImageTarget;
import io.quarkus.gradle.application.internal.planning.OutputLayout;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

class ExecutionRequestTest {

    @Test
    void buildRequestDefensivelyCopiesCollections() {
        var classpath = new ArrayList<Path>();
        classpath.add(Path.of("a.jar"));
        Map<String, String> forced = new HashMap<>();
        forced.put("quarkus.container-image.build", "true");

        var request = new BuildRequest(
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR),
                Path.of("build/quarkus-builds/app/package"),
                Path.of("build/app-model.dat"),
                classpath,
                Set.of(),
                new EffectiveConfigPlan(Map.of(), Map.of(), Map.of(), Map.of()),
                Map.of(),
                forced,
                true,
                new OutputLayout(Path.of("root"), Path.of("gen"), Path.of("app"), Optional.empty()));

        classpath.add(Path.of("b.jar"));
        forced.put("quarkus.container-image.push", "true");

        assertThat(request.classpath()).containsExactly(Path.of("a.jar"));
        assertThat(request.operationForcedProperties()).containsOnlyKeys("quarkus.container-image.build");
    }

    @Test
    void imageRequestRequiresExplicitOperation() {
        assertThatThrownBy(() -> new ImageRequest(
                buildRequest(),
                null,
                Optional.of(new ContainerImageTarget("quay.io/acme/app:1.0")),
                Optional.of(QuarkusApplicationImageBuilder.JIB),
                Map.of(),
                Map.of(),
                Path.of("receipt.properties"),
                Optional.empty(),
                Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operation");
    }

    @Test
    void imageRequestKeepsOperationKind() {
        var request = new ImageRequest(
                buildRequest(),
                ImageOperation.PUSH,
                Optional.of(new ContainerImageTarget("quay.io/acme/app:1.0")),
                Optional.of(QuarkusApplicationImageBuilder.JIB),
                Map.of(),
                Map.of(),
                Path.of("receipt.properties"),
                Optional.empty(),
                Optional.empty());

        assertThat(request.operation()).isEqualTo(ImageOperation.PUSH);
    }

    @Test
    void runRequestDefensivelyCopiesArguments() {
        var jvmArguments = new ArrayList<String>();
        jvmArguments.add("-Xmx128m");
        var applicationArguments = new ArrayList<String>();
        applicationArguments.add("--profile=test");
        Map<String, String> environment = new HashMap<>();
        environment.put("QUARKUS_LAUNCH_DEVMODE", "true");

        var request = new RunRequest(
                buildRequest(),
                Path.of("build/package-result.properties"),
                Optional.of("java"),
                jvmArguments,
                applicationArguments,
                environment,
                Path.of("."));

        jvmArguments.add("-Dlate=true");
        applicationArguments.add("--late=true");
        environment.put("LATE", "true");

        assertThat(request.runTarget()).contains("java");
        assertThat(request.jvmArguments()).containsExactly("-Xmx128m");
        assertThat(request.applicationArguments()).containsExactly("--profile=test");
        assertThat(request.environment()).containsExactly(Map.entry("QUARKUS_LAUNCH_DEVMODE", "true"));
    }

    @Test
    void startupOptimizedImageRequestRequiresBaseImageWorkingDirectory() {
        var baseImage = new BuiltContainerImage("jar-container", Optional.of(QuarkusApplicationImageBuilder.JIB), false,
                Optional.of("quay.io/acme/app:1.0"), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> new StartupOptimizedImageRequest(
                buildRequest(),
                ImageOperation.BUILD,
                baseImage,
                Path.of("image-result.properties"),
                QuarkusApplicationJvmStartupArchiveType.AOT,
                Path.of("app.aot"),
                "quay.io/acme/app:1.0-aot",
                Optional.of(QuarkusApplicationImageBuilder.JIB),
                Path.of("aot-result.properties")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("working directory");
    }

    @Test
    void startupOptimizedImageRequestKeepsTypedDirectoryArchive() {
        var baseImage = new BuiltContainerImage("jar-container", Optional.of(QuarkusApplicationImageBuilder.JIB), false,
                Optional.of("quay.io/acme/app:1.0"), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of("/work"), Optional.of("/work/quarkus-app"));
        Path archive = Path.of("build/training/app-scc");

        var request = new StartupOptimizedImageRequest(
                buildRequest(),
                ImageOperation.PUSH,
                baseImage,
                Path.of("image-result.properties"),
                QuarkusApplicationJvmStartupArchiveType.SCC,
                archive,
                "quay.io/acme/app:1.0-scc",
                Optional.of(QuarkusApplicationImageBuilder.JIB),
                Path.of("startup-optimized-result.properties"));

        assertThat(request.archiveType()).isEqualTo(QuarkusApplicationJvmStartupArchiveType.SCC);
        assertThat(request.archive()).isEqualTo(archive);
        assertThat(request.optimizedImageReference()).isEqualTo("quay.io/acme/app:1.0-scc");
    }

    @Test
    void startupOptimizedImageRequestRejectsBaseImageReferenceReuse() {
        var baseImage = new BuiltContainerImage("jar-container", Optional.of(QuarkusApplicationImageBuilder.JIB), false,
                Optional.of("quay.io/acme/app:1.0"), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of("/work"), Optional.of("/work/quarkus-app"));

        assertThatThrownBy(() -> new StartupOptimizedImageRequest(
                buildRequest(),
                ImageOperation.BUILD,
                baseImage,
                Path.of("image-result.properties"),
                QuarkusApplicationJvmStartupArchiveType.AOT,
                Path.of("app.aot"),
                "quay.io/acme/app:1.0",
                Optional.of(QuarkusApplicationImageBuilder.JIB),
                Path.of("startup-optimized-result.properties")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must differ");
    }

    private static BuildRequest buildRequest() {
        return new BuildRequest(
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR),
                Path.of("build/quarkus-builds/app/package"),
                Path.of("build/app-model.dat"),
                List.of(),
                Set.of(),
                new EffectiveConfigPlan(Map.of(), Map.of(), Map.of(), Map.of()),
                Map.of(),
                Map.of(),
                true,
                new OutputLayout(Path.of("root"), Path.of("gen"), Path.of("app"), Optional.empty()));
    }
}
