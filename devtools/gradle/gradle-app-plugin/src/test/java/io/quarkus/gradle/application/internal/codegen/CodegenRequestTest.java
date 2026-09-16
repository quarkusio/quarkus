package io.quarkus.gradle.application.internal.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.runtime.LaunchMode;

class CodegenRequestTest {

    @Test
    void defensivelyCopiesCollections() {
        Set<File> sourceParents = new HashSet<>();
        sourceParents.add(new File("src/main"));
        List<String> providers = new ArrayList<>(List.of("grpc"));
        List<String> inputNames = new ArrayList<>(List.of("proto"));
        List<Path> classpath = new ArrayList<>(List.of(Path.of("a.jar")));
        Map<String, String> buildSystemProperties = new HashMap<>();
        buildSystemProperties.put("quarkus.package.output-name", "app");

        CodegenRequest request = new CodegenRequest(
                Path.of("build/app-model.dat"),
                LaunchMode.NORMAL,
                sourceParents,
                Path.of("build/generated/sources/quarkus-application/main"),
                Path.of("build"),
                "app",
                providers,
                inputNames,
                classpath,
                new EffectiveConfigPlan(Map.of(), Map.of(), Map.of(), Map.of()),
                buildSystemProperties);

        sourceParents.add(new File("src/other"));
        providers.add("avro");
        inputNames.add("avro");
        classpath.add(Path.of("b.jar"));
        buildSystemProperties.put("quarkus.other", "value");

        assertThat(request.sourceParentDirectories()).containsExactly(new File("src/main"));
        assertThat(request.codegenProviders()).containsExactly("grpc");
        assertThat(request.codegenInputNames()).containsExactly("proto");
        assertThat(request.classpath()).containsExactly(Path.of("a.jar"));
        assertThat(request.buildSystemProperties()).containsOnlyKeys("quarkus.package.output-name");
    }

    @Test
    void requiresApplicationModel() {
        assertThatThrownBy(() -> new CodegenRequest(
                null,
                LaunchMode.NORMAL,
                Set.of(),
                Path.of("build/generated/sources/quarkus-application/main"),
                Path.of("build"),
                "app",
                List.of(),
                List.of(),
                List.of(),
                new EffectiveConfigPlan(Map.of(), Map.of(), Map.of(), Map.of()),
                Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("application model");
    }
}
