package io.quarkus.deployment.pkg.steps;

import java.io.IOException;
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
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.ProcessUtil;

public abstract class NativeImageBuildContainerRunner extends NativeImageBuildRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildContainerRunner.class);

    final NativeConfig nativeConfig;
    protected final ContainerRuntimeUtil.ContainerRuntime containerRuntime;
    String[] baseContainerRuntimeArgs;
    private final String containerName;

    protected NativeImageBuildContainerRunner(NativeConfig nativeConfig) {
        this.nativeConfig = nativeConfig;
        containerRuntime = ContainerRuntimeUtil.detectContainerRuntime();

        this.baseContainerRuntimeArgs = new String[] { "--env", "LANG=C", "--rm" };

        containerName = "build-native-" + RandomStringUtils.insecure().next(5, true, false);
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public void setup(boolean processInheritIODisabled) {
        if (containerRuntime != ContainerRuntimeUtil.ContainerRuntime.UNAVAILABLE) {
            log.infof("Using %s to run the native image builder", containerRuntime.getExecutableName());
            // we pull the docker image in order to give users an indication of which step the process is at
            // it's not strictly necessary we do this, however if we don't the subsequent version command
            // will appear to block and no output will be shown
            String effectiveBuilderImage = nativeConfig.builderImage().getEffectiveImage();
            var builderImagePull = nativeConfig.builderImage().pull();
            if (builderImagePull != NativeConfig.ImagePullStrategy.ALWAYS) {
                log.infof("Checking status of builder image '%s'", effectiveBuilderImage);
                Process imageInspectProcess = null;
                try {
                    final ProcessBuilder pb = new ProcessBuilder(
                            Arrays.asList(containerRuntime.getExecutableName(), "image", "inspect",
                                    "-f", "{{ .Id }}",
                                    effectiveBuilderImage))
                            // We only need the command's return status
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD);
                    imageInspectProcess = pb.start();
                    if (imageInspectProcess.waitFor() != 0) {
                        if (builderImagePull == NativeConfig.ImagePullStrategy.NEVER) {
                            throw new RuntimeException(
                                    "Could not find builder image '" + effectiveBuilderImage
                                            + "' locally and 'quarkus.native.builder-image.pull' is set to 'never'.");
                        } else {
                            log.infof("Could not find builder image '%s' locally, pulling the builder image",
                                    effectiveBuilderImage);
                        }
                    } else {
                        log.infof("Found builder image '%s' locally, skipping image pulling", effectiveBuilderImage);
                        return;
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("Failed to check status of builder image '" + effectiveBuilderImage + "'", e);
                } finally {
                    if (imageInspectProcess != null) {
                        imageInspectProcess.destroy();
                    }
                }
            }

            try {
                log.infof("Pulling builder image '%s'", effectiveBuilderImage);
                pull(effectiveBuilderImage, processInheritIODisabled);
            } catch (Exception e) {
                log.infof("Retrying in 5 seconds");
                try {
                    Thread.sleep(5_000L);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
                log.infof("Pulling builder image '%s' (take 2)", effectiveBuilderImage);
                pull(effectiveBuilderImage, processInheritIODisabled);
            }
        }
    }

    private void pull(String effectiveBuilderImage, boolean processInheritIODisabled) {
        Process pullProcess = null;
        try {
            final ProcessBuilder pb = new ProcessBuilder(
                    Arrays.asList(containerRuntime.getExecutableName(), "pull", effectiveBuilderImage));
            pullProcess = ProcessUtil.launchProcess(pb, processInheritIODisabled);
            if (pullProcess.waitFor() != 0) {
                throw new RuntimeException("Failed to pull builder image '" + effectiveBuilderImage + "'");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to pull builder image '" + effectiveBuilderImage + "'");
        } finally {
            if (pullProcess != null) {
                pullProcess.destroy();
            }
        }
    }

    @Override
    protected String[] getGraalVMVersionCommand(List<String> args) {
        List<String> containerRuntimeArgs;
        if (nativeConfig.containerRuntimeOptions().isPresent()) {
            List<String> runtimeOptions = nativeConfig.containerRuntimeOptions().get();
            containerRuntimeArgs = new ArrayList<>(runtimeOptions.size() + 1);
            containerRuntimeArgs.addAll(runtimeOptions);
            containerRuntimeArgs.add("--rm");
        } else {
            containerRuntimeArgs = Collections.singletonList("--rm");
        }
        return buildCommand("run", containerRuntimeArgs, args);
    }

    @Override
    protected String[] getBuildCommand(Path outputDir, List<String> args) {
        List<String> containerRuntimeBuildArgs = getContainerRuntimeBuildArgs(outputDir);
        List<String> effectiveContainerRuntimeBuildArgs = new ArrayList<>(containerRuntimeBuildArgs.size() + 2);
        effectiveContainerRuntimeBuildArgs.addAll(containerRuntimeBuildArgs);
        effectiveContainerRuntimeBuildArgs.add("--name");
        effectiveContainerRuntimeBuildArgs.add(containerName);
        return buildCommand("run", effectiveContainerRuntimeBuildArgs, args);
    }

    @Override
    protected void objcopy(Path outputDir, String... args) {
        final List<String> containerRuntimeBuildArgs = getContainerRuntimeBuildArgs(outputDir);
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

    protected List<String> getContainerRuntimeBuildArgs(Path outputDir) {
        List<String> containerRuntimeArgs = new ArrayList<>();
        nativeConfig.containerRuntimeOptions().ifPresent(containerRuntimeArgs::addAll);
        if (nativeConfig.debugBuildProcess() && nativeConfig.publishDebugBuildProcessPort()) {
            // publish the debug port onto the host if asked for
            containerRuntimeArgs.add("--publish=" + NativeImageBuildStep.DEBUG_BUILD_PROCESS_PORT + ":"
                    + NativeImageBuildStep.DEBUG_BUILD_PROCESS_PORT);
        }
        return containerRuntimeArgs;
    }

    protected String[] buildCommand(String dockerCmd, List<String> containerRuntimeArgs, List<String> command) {
        return Stream
                .of(Stream.of(containerRuntime.getExecutableName()), Stream.of(dockerCmd), Stream.of(baseContainerRuntimeArgs),
                        containerRuntimeArgs.stream(), Stream.of(nativeConfig.builderImage().getEffectiveImage()),
                        command.stream())
                .flatMap(Function.identity()).toArray(String[]::new);
    }

}
