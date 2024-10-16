package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.builder.JsonReader;
import io.quarkus.builder.json.JsonArray;
import io.quarkus.builder.json.JsonObject;
import io.quarkus.builder.json.JsonString;
import io.quarkus.builder.json.JsonValue;
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
        try {
            containerId = runCommandAndReadOutput(createTempContainerCommand).get(0);
        } catch (RuntimeException | InterruptedException | IOException e) {
            throw new RuntimeException("Failed to create temp container.", e);
        }
        // docker cp <files> <containerID>:/project
        final String[] copyCommand = new String[] {
                containerRuntime.getExecutableName(), "cp", outputDir.toAbsolutePath() + "/.",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH };
        runCommand(copyCommand, "Failed to copy source-jar and libs from host to builder container");
        super.preBuild(outputDir, buildArgs);
    }

    private List<String> runCommandAndReadOutput(String[] command) throws IOException, InterruptedException {
        log.info(String.join(" ", command).replace("$", "\\$"));
        final Process process = new ProcessBuilder(command).start();
        if (process.waitFor() != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines().toList();
        }
    }

    @Override
    protected void postBuild(Path outputDir, String nativeImageName, String resultingExecutableName) {
        copyFromContainerVolume(outputDir, resultingExecutableName,
                "Failed to copy native image from container volume back to the host.");

        // Note that podman cp does not support globbing i.e. cp /project/*.so will not work.
        // Why only .so? How about .dynlib and .lib? Regardless of the host platform,
        // the builder container is always Linux. So, we only need to copy .so files.
        //
        // We could either start the container again, exec `find' or `ls' to list the .so files,
        // stop the container again and use that list. We could also use the build-artifacts.json
        // to get the list of artifacts straight away which is what ended up doing here:
        copyFromContainerVolume(outputDir, "build-artifacts.json", null);
        try {
            final Path buildArtifactsFile = outputDir.resolve("build-artifacts.json");
            if (Files.exists(buildArtifactsFile)) {
                // The file is small enough to afford this read
                final String buildArtifactsJson = Files.readString(buildArtifactsFile);
                final JsonObject jsonRead = JsonReader.of(buildArtifactsJson).read();
                final JsonValue jdkLibraries = jsonRead.get("jdk_libraries");
                // The jdk_libraries field is optional, there might not be any.
                if (jdkLibraries instanceof JsonArray) {
                    for (JsonValue lib : ((JsonArray) jdkLibraries).value()) {
                        copyFromContainerVolume(outputDir, ((JsonString) lib).value(),
                                "Failed to copy " + lib + " from container volume back to the host.");
                    }
                }
            }
        } catch (IOException e) {
            log.errorf(e, "Failed to list .so files in the build-artifacts.json. Skipping the step.");
        }

        if (nativeConfig.debug().enabled()) {
            copyFromContainerVolume(outputDir, "sources",
                    "Failed to copy sources from container volume back to the host.");
            final String symbols = String.format("%s.debug", nativeImageName);
            copyFromContainerVolume(outputDir, symbols,
                    "Failed to copy debug symbols from container volume back to the host.");
        }
        // docker container rm <containerID>
        final String[] rmTempContainerCommand = new String[] { containerRuntime.getExecutableName(), "container", "rm",
                containerId };
        runCommand(rmTempContainerCommand, "Failed to remove container: " + containerId);
        // docker volume rm <volumeID>
        rmVolume("Failed to remove volume: " + CONTAINER_BUILD_VOLUME_NAME);
    }

    private void rmVolume(String errorMsg) {
        final String[] rmVolumeCommand = new String[] { containerRuntime.getExecutableName(), "volume", "rm",
                CONTAINER_BUILD_VOLUME_NAME };
        runCommand(rmVolumeCommand, errorMsg);
    }

    private void copyFromContainerVolume(Path outputDir, String path, String errorMsg) {
        // docker cp <containerID>:/project/<path> <dest>
        final String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + "/" + path,
                outputDir.toAbsolutePath().toString() };
        runCommand(copyCommand, errorMsg);
    }

    @Override
    protected List<String> getContainerRuntimeBuildArgs(Path outputDir) {
        final List<String> containerRuntimeArgs = super.getContainerRuntimeBuildArgs(outputDir);
        Collections.addAll(containerRuntimeArgs, "-v",
                CONTAINER_BUILD_VOLUME_NAME + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH);
        return containerRuntimeArgs;
    }
}
