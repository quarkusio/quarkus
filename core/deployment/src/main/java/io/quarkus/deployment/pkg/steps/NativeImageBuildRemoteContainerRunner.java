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

    private final String nativeImageName;
    private final String resultingExecutableName;
    private String containerId;

    public NativeImageBuildRemoteContainerRunner(NativeConfig nativeConfig, Path outputDir,
            String nativeImageName, String resultingExecutableName) {
        super(nativeConfig, outputDir);
        this.nativeImageName = nativeImageName;
        this.resultingExecutableName = resultingExecutableName;
    }

    @Override
    protected void preBuild(List<String> buildArgs) throws InterruptedException, IOException {
        // docker volume rm <volumeID>
        rmVolume(null);
        // docker create -v <volumeID>:/project <image-name>
        final List<String> containerRuntimeArgs = Arrays.asList("-v",
                CONTAINER_BUILD_VOLUME_NAME + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH);
        final String[] createTempContainerCommand = buildCommand("create", containerRuntimeArgs, Collections.emptyList());
        containerId = runCommandAndReadOutput(createTempContainerCommand, "Failed to create temp container.");
        // docker cp <files> <containerID>:/project
        String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp", outputPath + "/.",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH };
        runCommand(copyCommand, "Failed to copy source-jar and libs from host to builder container", null);
        super.preBuild(buildArgs);
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
    protected void postBuild() {
        copyFromContainerVolume(resultingExecutableName, "Failed to copy native image from container volume back to the host.");
        if (nativeConfig.debug.enabled) {
            copyFromContainerVolume("sources", "Failed to copy sources from container volume back to the host.");
            String symbols = String.format("%s.debug", nativeImageName);
            copyFromContainerVolume(symbols, "Failed to copy debug symbols from container volume back to the host.");
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

    private void copyFromContainerVolume(String path, String errorMsg) {
        // docker cp <containerID>:/project/<path> <dest>
        String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + "/" + path, outputPath };
        runCommand(copyCommand, errorMsg, null);
    }

    @Override
    protected List<String> getContainerRuntimeBuildArgs() {
        List<String> containerRuntimeArgs = super.getContainerRuntimeBuildArgs();
        Collections.addAll(containerRuntimeArgs, "-v",
                CONTAINER_BUILD_VOLUME_NAME + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH);
        return containerRuntimeArgs;
    }
}
