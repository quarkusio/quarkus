package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;

class NativeImageBuildContainerRunnerTest {

    @Test
    void testBuilderImageBeingPickedUp() {
        NativeConfig nativeConfig = new NativeConfig();
        nativeConfig.containerRuntime = Optional.empty();
        boolean found;
        NativeImageBuildLocalContainerRunner localRunner;
        String[] command;

        nativeConfig.builderImage = "graalvm";
        localRunner = new NativeImageBuildLocalContainerRunner(nativeConfig, Path.of("/tmp"),
                CompiledJavaVersionBuildItem.JavaVersion.Unknown.INSTANCE);
        command = localRunner.buildCommand("docker", Collections.emptyList(), Collections.emptyList());
        found = false;
        for (String part : command) {
            if (part.contains("ubi-quarkus-native-image")) {
                found = true;
            }
        }
        assertThat(found).isTrue();

        nativeConfig.builderImage = "mandrel";
        localRunner = new NativeImageBuildLocalContainerRunner(nativeConfig, Path.of("/tmp"),
                CompiledJavaVersionBuildItem.JavaVersion.Unknown.INSTANCE);
        command = localRunner.buildCommand("docker", Collections.emptyList(), Collections.emptyList());
        found = false;
        for (String part : command) {
            if (part.contains("ubi-quarkus-mandrel")) {
                found = true;
            }
        }
        assertThat(found).isTrue();

        nativeConfig.builderImage = "RandomString";
        localRunner = new NativeImageBuildLocalContainerRunner(nativeConfig, Path.of("/tmp"),
                CompiledJavaVersionBuildItem.JavaVersion.Unknown.INSTANCE);
        command = localRunner.buildCommand("docker", Collections.emptyList(), Collections.emptyList());
        found = false;
        for (String part : command) {
            if (part.equals("RandomString")) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }
}
