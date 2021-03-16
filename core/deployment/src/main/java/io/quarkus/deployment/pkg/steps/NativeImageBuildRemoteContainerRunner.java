package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.NativeConfig;

public class NativeImageBuildRemoteContainerRunner extends NativeImageBuildContainerRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildRemoteContainerRunner.class);

    private final String resultingExecutableName;
    private String containerId;

    public NativeImageBuildRemoteContainerRunner(NativeConfig nativeConfig, Path outputDir, String resultingExecutableName) {
        super(nativeConfig, outputDir);
        this.resultingExecutableName = resultingExecutableName;
    }

    @Override
    protected void preBuild(List<String> buildArgs) throws InterruptedException, IOException {
        List<String> containerRuntimeArgs = getContainerRuntimeBuildArgs();
        String[] createContainerCommand = buildCommand("create", containerRuntimeArgs, buildArgs);
        log.info(String.join(" ", createContainerCommand).replace("$", "\\$"));
        Process createContainerProcess = new ProcessBuilder(createContainerCommand).start();
        if (createContainerProcess.waitFor() != 0) {
            throw new RuntimeException("Failed to create builder container.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(createContainerProcess.getInputStream()))) {
            containerId = reader.readLine();
        }
        String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp", outputPath + "/.",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH };
        runCommand(copyCommand, "Failed to copy source-jar and libs from host to builder container", null);
        super.preBuild(buildArgs);
    }

    @Override
    protected String[] getBuildCommand(List<String> args) {
        return new String[] { containerRuntime.getExecutableName(), "start", "--attach", containerId };
    }

    @Override
    protected void postBuild() throws InterruptedException, IOException {
        copyFromBuilder(resultingExecutableName, "Failed to copy native executable from container back to the host.");
        if (nativeConfig.debug.enabled) {
            copyFromBuilder("sources", "Failed to copy sources from container back to the host.");
        }
        String[] removeCommand = new String[] { containerRuntime.getExecutableName(), "container", "rm", "--volumes",
                containerId };
        runCommand(removeCommand, "Failed to remove container: " + containerId, null);
    }

    private void copyFromBuilder(String path, String errorMsg) throws IOException, InterruptedException {
        String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + "/" + path, outputPath };
        runCommand(copyCommand, errorMsg, null);
    }
}
