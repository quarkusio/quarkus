package io.quarkus.gradle.application.internal.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.JarResult;

class PackageLayoutInferencePlannerTest {

    private final PackageLayoutInferencePlanner planner = new PackageLayoutInferencePlanner();

    @Test
    void extractsAvailableAugmentationFacts() {
        var jar = new JarResult(Path.of("build/app-runner.jar"), Path.of("build/original.jar"), Path.of("build/lib"),
                false, null);
        var result = new AugmentResult(
                List.of(new ArtifactResult(Path.of("build/app-runner.jar"), "jar", Map.of())),
                jar,
                Path.of("build/app-runner"),
                Map.of());

        var facts = planner.facts(result);

        assertThat(facts.artifactPaths()).containsOnly(Path.of("build/app-runner.jar"));
        assertThat(facts.jarPath()).contains(Path.of("build/app-runner.jar"));
        assertThat(facts.libraryDirectory()).contains(Path.of("build/lib"));
        assertThat(facts.nativeResult()).contains(Path.of("build/app-runner"));
        assertThat(facts.requiresLayoutInference()).isFalse();
    }

    @Test
    void marksMissingResultMetadataAsNeedingInference() {
        var facts = planner.facts(new AugmentResult(null, null, null, Map.of()));

        assertThat(facts.artifactPaths()).isEmpty();
        assertThat(facts.jarPath()).isEmpty();
        assertThat(facts.libraryDirectory()).isEmpty();
        assertThat(facts.nativeResult()).isEmpty();
        assertThat(facts.requiresLayoutInference()).isTrue();
    }
}
