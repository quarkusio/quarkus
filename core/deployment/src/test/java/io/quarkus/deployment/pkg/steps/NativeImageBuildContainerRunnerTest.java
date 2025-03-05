package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

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
            if (part.contains("ubi9-quarkus-graalvmce-builder-image")) {
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
            if (part.contains("ubi9-quarkus-mandrel-builder-image")) {
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

}
