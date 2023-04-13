package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.NativeConfig;

public class NativeImageBuildRemoteContainerRunner extends NativeImageBuildContainerRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildRemoteContainerRunner.class);
    // Use a predefined volume name and implicitly create the volume with `podman create`, instead of explicitly
    // generating a unique volume with `podman volume create`, to work around Podman's 3.0.x
    // issue https://github.com/containers/podman/issues/9608
    private static final String CONTAINER_BUILD_VOLUME_NAME = "quarkus-native-builder-image-project-volume";

    private String containerId;

    protected NativeImageBuildRemoteContainerRunner(NativeConfig nativeConfig) {
        super(nativeConfig);
    }

    @Override
    protected void preBuild(Path outputDir, List<String> buildArgs) throws InterruptedException, IOException {
        // docker volume rm <volumeID>
        rmVolume(null);
        // docker create -v <volumeID>:/project <image-name>
        final List<String> containerRuntimeArgs = Arrays.asList("-v",
                CONTAINER_BUILD_VOLUME_NAME + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH);
        final String[] createTempContainerCommand = buildCommand("create", containerRuntimeArgs, Collections.emptyList());
        containerId = runCommandAndReadOutput(createTempContainerCommand, "Failed to create temp container.");
        // docker cp <files> <containerID>:/project
        String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp", outputDir.toAbsolutePath() + "/.",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH };
        runCommand(copyCommand, "Failed to copy source-jar and libs from host to builder container", null);
        super.preBuild(outputDir, buildArgs);
    }

    private String runCommandAndReadOutput(String[] command, String errorMsg) throws IOException, InterruptedException {
        log.info(String.join(" ", command).replace("$", "\\$"));
        Process process = new ProcessBuilder(command).start();
        if (process.waitFor() != 0) {
            throw new RuntimeException(errorMsg);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.readLine();
        }
    }

    @Override
    protected void postBuild(Path outputDir, String nativeImageName, String resultingExecutableName) {
        copyFromContainerVolume(outputDir, resultingExecutableName,
                "Failed to copy native image from container volume back to the host.");
        if (nativeConfig.debug().enabled()) {
            copyFromContainerVolume(outputDir, "sources", "Failed to copy sources from container volume back to the host.");
            String symbols = String.format("%s.debug", nativeImageName);
            copyFromContainerVolume(outputDir, symbols, "Failed to copy debug symbols from container volume back to the host.");
        }
        // docker container rm <containerID>
        final String[] rmTempContainerCommand = new String[] { containerRuntime.getExecutableName(), "container", "rm",
                containerId };
        runCommand(rmTempContainerCommand, "Failed to remove container: " + containerId, null);
        // docker volume rm <volumeID>
        rmVolume("Failed to remove volume: " + CONTAINER_BUILD_VOLUME_NAME);
    }

    private void rmVolume(String errorMsg) {
        final String[] rmVolumeCommand = new String[] { containerRuntime.getExecutableName(), "volume", "rm",
                CONTAINER_BUILD_VOLUME_NAME };
        runCommand(rmVolumeCommand, errorMsg, null);
    }

    private void copyFromContainerVolume(Path outputDir, String path, String errorMsg) {
        // docker cp <containerID>:/project/<path> <dest>
        String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + "/" + path,
                outputDir.toAbsolutePath().toString() };
        runCommand(copyCommand, errorMsg, null);
    }

    @Override
    protected List<String> getContainerRuntimeBuildArgs(Path outputDir) {
        List<String> containerRuntimeArgs = super.getContainerRuntimeBuildArgs(outputDir);
        Collections.addAll(containerRuntimeArgs, "-v",
                CONTAINER_BUILD_VOLUME_NAME + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH);
        return containerRuntimeArgs;
    }
}
