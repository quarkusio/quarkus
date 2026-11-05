package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.TestNativeConfig;
import io.quarkus.deployment.util.ContainerRuntimeUtil;

class NativeImageBuildContainerRunnerTest {

    // This will default to false in the maven build and true in the IDE, so this will still run if invoked explicitly
    @DisabledIfSystemProperty(named = "avoid-containers", matches = "true")
    @Test
    void testBuilderImageBeingPickedUp() {
        ContainerRuntimeUtil.ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime(true);

        NativeConfig nativeConfig = new TestNativeConfig("graalvm");
        boolean found;
        NativeImageBuildLocalContainerRunner localRunner;
        String[] command;

        localRunner = new NativeImageBuildLocalContainerRunner(nativeConfig);
        command = localRunner.buildCommand(containerRuntime.getExecutableName(), Collections.emptyList(),
                Collections.emptyList());
        found = false;
        for (String part : command) {
            if (part.contains("ubi10-quarkus-graalvmce-builder-image")) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();

        nativeConfig = new TestNativeConfig("mandrel");
        localRunner = new NativeImageBuildLocalContainerRunner(nativeConfig);
        command = localRunner.buildCommand(containerRuntime.getExecutableName(), Collections.emptyList(),
                Collections.emptyList());
        found = false;
        for (String part : command) {
            if (part.contains("ubi10-quarkus-mandrel-builder-image")) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();

        nativeConfig = new TestNativeConfig("RandomString");
        localRunner = new NativeImageBuildLocalContainerRunner(nativeConfig);
        command = localRunner.buildCommand(containerRuntime.getExecutableName(), Collections.emptyList(),
                Collections.emptyList());
        found = false;
        for (String part : command) {
            if (part.equals("RandomString")) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @DisabledIfSystemProperty(named = "avoid-containers", matches = "true")
    @Test
    void buildContainerRunsWithoutRmSoTheOomFlagCanBeInspected() {
        NativeImageBuildLocalContainerRunner runner = new NativeImageBuildLocalContainerRunner(new TestNativeConfig("mandrel"));
        // The build container is NOT run with --rm: Quarkus removes it itself, after inspecting State.OOMKilled.
        assertThat(runner.getBuildCommand(Path.of("target"), Collections.emptyList())).doesNotContain("--rm");
        // The short-lived version container is still auto-removed.
        assertThat(runner.getGraalVMVersionCommand(Collections.singletonList("--version"))).contains("--rm");
    }

}
