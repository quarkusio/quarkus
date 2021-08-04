package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.ProcessUtil;

public abstract class NativeImageBuildContainerRunner extends NativeImageBuildRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildContainerRunner.class);

    final NativeConfig nativeConfig;
    protected final NativeConfig.ContainerRuntime containerRuntime;
    private final String[] baseContainerRuntimeArgs;
    protected final String outputPath;
    private final String containerName;

    public NativeImageBuildContainerRunner(NativeConfig nativeConfig, Path outputDir) {
        this.nativeConfig = nativeConfig;
        containerRuntime = nativeConfig.containerRuntime.orElseGet(NativeImageBuildContainerRunner::detectContainerRuntime);
        log.infof("Using %s to run the native image builder", containerRuntime.getExecutableName());

        this.baseContainerRuntimeArgs = new String[] { "--env", "LANG=C", "--rm" };

        outputPath = outputDir == null ? null : outputDir.toAbsolutePath().toString();
        containerName = "build-native-" + RandomStringUtils.random(5, true, false);
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
        return buildCommand("run", Collections.singletonList("--rm"), args);
    }

    @Override
    protected String[] getBuildCommand(List<String> args) {
        List<String> containerRuntimeBuildArgs = getContainerRuntimeBuildArgs();
        List<String> effectiveContainerRuntimeBuildArgs = new ArrayList<>(containerRuntimeBuildArgs.size() + 2);
        effectiveContainerRuntimeBuildArgs.addAll(containerRuntimeBuildArgs);
        effectiveContainerRuntimeBuildArgs.add("--name");
        effectiveContainerRuntimeBuildArgs.add(containerName);
        return buildCommand("run", effectiveContainerRuntimeBuildArgs, args);
    }

    @Override
    protected void objcopy(String... args) {
        final List<String> containerRuntimeBuildArgs = getContainerRuntimeBuildArgs();
        Collections.addAll(containerRuntimeBuildArgs, "--entrypoint", "/bin/bash");
        final ArrayList<String> objcopyCommand = new ArrayList<>(2);
        objcopyCommand.add("-c");
        objcopyCommand.add("objcopy " + String.join(" ", args));
        final String[] command = buildCommand("run", containerRuntimeBuildArgs, objcopyCommand);
        runCommand(command, null, null);
    }

    @Override
    public void addShutdownHook(Process process) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (process.isAlive()) {
                try {
                    Process removeProcess = new ProcessBuilder(
                            List.of(containerRuntime.getExecutableName(), "rm", "-f", containerName))
                                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                                    .start();
                    removeProcess.waitFor(2, TimeUnit.SECONDS);
                } catch (IOException | InterruptedException e) {
                    log.debug("Unable to stop running container", e);
                }
            }
        }));
    }

    protected List<String> getContainerRuntimeBuildArgs() {
        List<String> containerRuntimeArgs = new ArrayList<>();
        nativeConfig.containerRuntimeOptions.ifPresent(containerRuntimeArgs::addAll);
        if (nativeConfig.debugBuildProcess && nativeConfig.publishDebugBuildProcessPort) {
            // publish the debug port onto the host if asked for
            containerRuntimeArgs.add("--publish=" + NativeImageBuildStep.DEBUG_BUILD_PROCESS_PORT + ":"
                    + NativeImageBuildStep.DEBUG_BUILD_PROCESS_PORT);
        }
        return containerRuntimeArgs;
    }

    protected String[] buildCommand(String dockerCmd, List<String> containerRuntimeArgs, List<String> command) {
        return Stream
                .of(Stream.of(containerRuntime.getExecutableName()), Stream.of(dockerCmd), Stream.of(baseContainerRuntimeArgs),
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
