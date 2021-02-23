package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.deployment.pkg.NativeConfig;

public class NativeImageBuildRemoteContainerRunner extends NativeImageBuildContainerRunner {

    private final String nativeImageName;
    private String containerId;

    public NativeImageBuildRemoteContainerRunner(NativeConfig nativeConfig, Path outputDir, String nativeImageName) {
        super(nativeConfig, outputDir);
        this.nativeImageName = nativeImageName;
    }

    @Override
    protected void preBuild(List<String> buildArgs) throws InterruptedException, IOException {
        List<String> containerRuntimeArgs = getContainerRuntimeBuildArgs();
        String[] createContainerCommand = buildCommand("create", containerRuntimeArgs, buildArgs);
        Process createContainerProcess = new ProcessBuilder(createContainerCommand).start();
        createContainerProcess.waitFor();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(createContainerProcess.getInputStream()))) {
            containerId = reader.readLine();
        }
        String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp", outputPath + "/.",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH };
        Process copyProcess = new ProcessBuilder(copyCommand).start();
        copyProcess.waitFor();
        super.preBuild(buildArgs);
    }

    @Override
    protected String[] getBuildCommand(List<String> args) {
        return new String[] { containerRuntime.getExecutableName(), "start", "--attach", containerId };
    }

    @Override
    protected void postBuild() throws InterruptedException, IOException {
        copy(nativeImageName);
        if (nativeConfig.debug.enabled) {
            copy("sources");
        }
        String[] removeCommand = new String[] { containerRuntime.getExecutableName(), "container", "rm", "--volumes",
                containerId };
        Process removeProcess = new ProcessBuilder(removeCommand).start();
        removeProcess.waitFor();
    }

    private void copy(String path) throws IOException, InterruptedException {
        String[] copyCommand = new String[] { containerRuntime.getExecutableName(), "cp",
                containerId + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + "/" + path, outputPath };
        Process copyProcess = new ProcessBuilder(copyCommand).start();
        copyProcess.waitFor();
    }
}
