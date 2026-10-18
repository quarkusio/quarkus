package io.quarkus.deployment.pkg.steps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.smallrye.common.process.ProcessBuilder;

public abstract class NativeImageBuildContainerRunner extends NativeImageBuildRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildContainerRunner.class);

    final NativeConfig nativeConfig;
    protected final ContainerRuntimeUtil.ContainerRuntime containerRuntime;
    String[] baseContainerRuntimeArgs;
    private final String containerName;
    private final AtomicBoolean setupInvoked = new AtomicBoolean();

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
        if (containerRuntime == ContainerRuntimeUtil.ContainerRuntime.UNAVAILABLE) {
            return;
        }
        if (setupInvoked.compareAndSet(false, true)) {
            log.infof("Using %s to run the native image builder", containerRuntime.getExecutableName());
            // we pull the docker image in order to give users an indication of which step the process is at
            // it's not strictly necessary we do this, however if we don't the subsequent version command
            // will appear to block and no output will be shown
            String effectiveBuilderImage = nativeConfig.builderImage().getEffectiveImage();
            var builderImagePull = nativeConfig.builderImage().pull();
            if (builderImagePull != NativeConfig.ImagePullStrategy.ALWAYS) {
                log.infof("Checking status of builder image '%s'", effectiveBuilderImage);
                try {
                    var holder = new Object() {
                        int exitCode;
                    };
                    ProcessBuilder.newBuilder(containerRuntime.getExecutableName())
                            .arguments("image", "inspect", "-f", "{{ .Id }}", effectiveBuilderImage)
                            .exitCodeChecker(ec -> {
                                holder.exitCode = ec;
                                return true;
                            })
                            .run();
                    if (holder.exitCode != 0) {
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
                } catch (Exception e) {
                    throw new RuntimeException("Failed to check status of builder image '" + effectiveBuilderImage + "'", e);
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
        var pb = ProcessBuilder.newBuilder(containerRuntime.getExecutableName())
                .arguments("pull", effectiveBuilderImage);
        // todo: maybe this should just be logged or something?
        if (processInheritIODisabled) {
            pb.output().consumeLinesWith(8192, System.out::println);
            // logOnSuccess(false) avoids WARNING from io.smallrye.common.process.Logging
            pb.error().logOnSuccess(false).consumeLinesWith(8192, System.err::println);
        } else {
            pb.output().inherited();
            // logOnSuccess(false) avoids WARNING from io.smallrye.common.process.Logging
            pb.error().logOnSuccess(false).inherited();
        }
        try {
            pb.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to pull builder image '" + effectiveBuilderImage + "'", e);
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
