package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.ProcessUtil;

public class NativeImageBuildContainerRunner extends NativeImageBuildRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildContainerRunner.class);

    private final NativeConfig nativeConfig;
    private final NativeConfig.ContainerRuntime containerRuntime;
    private final String[] baseContainerRuntimeArgs;
    private final String outputPath;

    public NativeImageBuildContainerRunner(NativeConfig nativeConfig, Path outputDir) {
        this.nativeConfig = nativeConfig;
        containerRuntime = nativeConfig.containerRuntime.orElseGet(NativeImageBuildContainerRunner::detectContainerRuntime);
        log.infof("Using %s to run the native image builder", containerRuntime.getExecutableName());

        List<String> containerRuntimeArgs = new ArrayList<>();
        Collections.addAll(containerRuntimeArgs, "run", "--env", "LANG=C");

        String outputPath = outputDir.toAbsolutePath().toString();
        if (SystemUtils.IS_OS_WINDOWS) {
            outputPath = FileUtil.translateToVolumePath(outputPath);
        }
        this.outputPath = outputPath;

        if (SystemUtils.IS_OS_LINUX) {
            String uid = getLinuxID("-ur");
            String gid = getLinuxID("-gr");
            if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                Collections.addAll(containerRuntimeArgs, "--user", uid + ":" + gid);
                if (containerRuntime == NativeConfig.ContainerRuntime.PODMAN) {
                    // Needed to avoid AccessDeniedExceptions
                    containerRuntimeArgs.add("--userns=keep-id");
                }
            }
        }
        Collections.addAll(containerRuntimeArgs, "--rm");
        this.baseContainerRuntimeArgs = containerRuntimeArgs.toArray(new String[0]);
    }

    @Override
    public void setup(boolean processInheritIODisabled) {
        if (containerRuntime == NativeConfig.ContainerRuntime.DOCKER
                || containerRuntime == NativeConfig.ContainerRuntime.PODMAN) {
            // we pull the docker image in order to give users an indication of which step the process is at
            // it's not strictly necessary we do this, however if we don't the subsequent version command
            // will appear to block and no output will be shown
            log.info("Checking image status " + nativeConfig.builderImage);
            Process pullProcess = null;
            try {
                final ProcessBuilder pb = new ProcessBuilder(
                        Arrays.asList(containerRuntime.getExecutableName(), "pull", nativeConfig.builderImage));
                pullProcess = ProcessUtil.launchProcess(pb, processInheritIODisabled);
                pullProcess.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to pull builder image " + nativeConfig.builderImage, e);
            } finally {
                if (pullProcess != null) {
                    pullProcess.destroy();
                }
            }
        }
    }

    @Override
    protected String[] getGraalVMVersionCommand(List<String> args) {
        return buildCommand(Collections.emptyList(), args);
    }

    @Override
    protected String[] getBuildCommand(List<String> args) {
        List<String> containerRuntimeArgs = new ArrayList<>();
        Collections.addAll(containerRuntimeArgs, "-v",
                outputPath + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + ":z");
        nativeConfig.containerRuntimeOptions.ifPresent(containerRuntimeArgs::addAll);
        if (nativeConfig.debugBuildProcess && nativeConfig.publishDebugBuildProcessPort) {
            // publish the debug port onto the host if asked for
            containerRuntimeArgs.add("--publish=" + NativeImageBuildStep.DEBUG_BUILD_PROCESS_PORT + ":"
                    + NativeImageBuildStep.DEBUG_BUILD_PROCESS_PORT);
        }
        return buildCommand(containerRuntimeArgs, args);
    }

    private String[] buildCommand(List<String> containerRuntimeArgs, List<String> command) {
        return Stream
                .of(Stream.of(containerRuntime.getExecutableName()), Stream.of(baseContainerRuntimeArgs),
                        containerRuntimeArgs.stream(), Stream.of(nativeConfig.builderImage), command.stream())
                .flatMap(Function.identity()).toArray(String[]::new);
    }

    /**
     * @return {@link NativeConfig.ContainerRuntime#DOCKER} if it's available, or {@link NativeConfig.ContainerRuntime#PODMAN}
     *         if the podman
     *         executable exists in the environment or if the docker executable is an alias to podman
     * @throws IllegalStateException if no container runtime was found to build the image
     */
    private static NativeConfig.ContainerRuntime detectContainerRuntime() {
        // Docker version 19.03.14, build 5eb3275d40
        String dockerVersionOutput = getVersionOutputFor(NativeConfig.ContainerRuntime.DOCKER);
        boolean dockerAvailable = dockerVersionOutput.contains("Docker version");
        // Check if Podman is installed
        // podman version 2.1.1
        String podmanVersionOutput = getVersionOutputFor(NativeConfig.ContainerRuntime.PODMAN);
        boolean podmanAvailable = podmanVersionOutput.startsWith("podman version");
        if (dockerAvailable) {
            // Check if "docker" is an alias to "podman"
            if (dockerVersionOutput.equals(podmanVersionOutput)) {
                return NativeConfig.ContainerRuntime.PODMAN;
            }
            return NativeConfig.ContainerRuntime.DOCKER;
        } else if (podmanAvailable) {
            return NativeConfig.ContainerRuntime.PODMAN;
        } else {
            throw new IllegalStateException("No container runtime was found to run the native image builder");
        }
    }

    private static String getVersionOutputFor(NativeConfig.ContainerRuntime containerRuntime) {
        Process versionProcess = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(containerRuntime.getExecutableName(), "--version")
                    .redirectErrorStream(true);
            versionProcess = pb.start();
            versionProcess.waitFor();
            return new String(FileUtil.readFileContents(versionProcess.getInputStream()), StandardCharsets.UTF_8);
        } catch (IOException | InterruptedException e) {
            // If an exception is thrown in the process, just return an empty String
            log.debugf(e, "Failure to read version output from %s", containerRuntime.getExecutableName());
            return "";
        } finally {
            if (versionProcess != null) {
                versionProcess.destroy();
            }
        }
    }

}
