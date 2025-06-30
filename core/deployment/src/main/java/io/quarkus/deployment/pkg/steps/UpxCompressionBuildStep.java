package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.builditem.UpxCompressedBuildItem;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.deployment.util.FileUtil;
import io.smallrye.common.process.AbnormalExitException;
import io.smallrye.common.process.ProcessBuilder;
import io.smallrye.common.process.ProcessUtil;

public class UpxCompressionBuildStep {

    private static final Logger log = Logger.getLogger(UpxCompressionBuildStep.class);

    @BuildStep(onlyIf = NativeBuild.class)
    public void compress(NativeConfig nativeConfig, NativeImageRunnerBuildItem nativeImageRunner,
            NativeImageBuildItem image,
            BuildProducer<UpxCompressedBuildItem> upxCompressedProducer,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {

        if (nativeConfig.compression().level().isEmpty() || !nativeConfig.compression().enabled()) {
            log.debug("UPX compression disabled");
            return;
        }
        if (image.isReused()) {
            log.debug("Native executable reused: skipping compression");
            return;
        }

        String effectiveBuilderImage = nativeConfig.builderImage().getEffectiveImage();
        Optional<File> upxPathFromSystem = getUpxFromSystem();
        if (upxPathFromSystem.isPresent() && !nativeConfig.compression().containerBuild().orElse(false)
                && nativeConfig.compression().containerImage().isEmpty()) {
            log.debug("Running UPX from system path");
            if (!runUpxFromHost(upxPathFromSystem.get(), image.getPath().toFile(), nativeConfig)) {
                throw new IllegalStateException("Unable to compress the native executable");
            }
        } else if (nativeConfig.remoteContainerBuild()) {
            log.error("Compression of native executables is not yet implemented for remote container builds.");
            throw new IllegalStateException(
                    "Unable to compress the native executable: Compression of native executables is not yet supported for remote container builds");
        } else if (nativeConfig.compression().containerBuild().orElse(true) &&
                (nativeImageRunner.isContainerBuild() ||
                        nativeConfig.compression().containerImage().isPresent())) {
            String compressorImage = nativeConfig.compression().containerImage().orElse(effectiveBuilderImage);
            log.info("Running UPX from a container using the compressor image: " + compressorImage);
            if (!runUpxInContainer(image, nativeConfig, compressorImage)) {
                throw new IllegalStateException("Unable to compress the native executable");
            }
        } else {
            log.error("Unable to compress the native executable. Either install `upx` from https://upx.github.io/" +
                    " on your machine, or enable in-container build using `-Dquarkus.native.container-build=true`.");
            throw new IllegalStateException("Unable to compress the native executable: `upx` not available");
        }
        log.infof("Native executable compressed: %s", image.getPath().toFile().getAbsolutePath());
        upxCompressedProducer.produce(new UpxCompressedBuildItem());
    }

    private boolean runUpxFromHost(File upx, File executable, NativeConfig nativeConfig) {
        List<String> extraArgs = nativeConfig.compression().additionalArgs().orElse(List.of());
        List<String> args = Stream.concat(
                Stream.concat(
                        nativeConfig.compression().level().stream().mapToObj(this::getCompressionLevel),
                        extraArgs.stream()),
                Stream.of(executable.getAbsolutePath())).toList();
        log.infof("Executing %s", String.join(" ", args));
        try {
            ProcessBuilder.newBuilder(upx.getAbsolutePath())
                    .arguments(args)
                    .directory(executable.getAbsoluteFile().getParentFile().toPath())
                    .output().consumeLinesWith(8192, System.out::println)
                    .error().consumeLinesWith(8192, System.err::println)
                    .run();
        } catch (Exception e) {
            log.errorf(e, "Command %s failed", String.join(" ", args));
            return false;
        }
        return true;
    }

    private boolean runUpxInContainer(NativeImageBuildItem nativeImage, NativeConfig nativeConfig,
            String effectiveBuilderImage) {
        List<String> extraArgs = nativeConfig.compression().additionalArgs().orElse(Collections.emptyList());

        List<String> commandLine = new ArrayList<>();
        ContainerRuntimeUtil.ContainerRuntime containerRuntime = ContainerRuntimeUtil.detectContainerRuntime();
        commandLine.add(containerRuntime.getExecutableName());

        commandLine.add("run");
        commandLine.add("--env");
        commandLine.add("LANG=C");
        commandLine.add("--rm");
        commandLine.add("--entrypoint=upx");

        String containerName = "upx-" + RandomStringUtils.insecure().next(5, true, false);
        commandLine.add("--name");
        commandLine.add(containerName);

        String volumeOutputPath = nativeImage.getPath().toFile().getParentFile().getAbsolutePath();
        if (SystemUtils.IS_OS_WINDOWS) {
            volumeOutputPath = FileUtil.translateToVolumePath(volumeOutputPath);
        } else if (SystemUtils.IS_OS_LINUX) {
            String uid = getLinuxID("-ur");
            String gid = getLinuxID("-gr");
            if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                Collections.addAll(commandLine, "--user", uid + ":" + gid);
                if (containerRuntime.isPodman() && containerRuntime.isRootless()) {
                    // Needed to avoid AccessDeniedExceptions
                    commandLine.add("--userns=keep-id");
                }
            }
        }

        Collections.addAll(commandLine, "-v",
                volumeOutputPath + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + ":z");

        commandLine.add(effectiveBuilderImage);
        if (nativeConfig.compression().level().isPresent()) {
            commandLine.add(getCompressionLevel(nativeConfig.compression().level().getAsInt()));
        }
        commandLine.addAll(extraArgs);

        commandLine.add(nativeImage.getPath().toFile().getName());

        log.infof("Compress native executable using: %s", String.join(" ", commandLine));
        try {
            ProcessBuilder.newBuilder(commandLine.get(0))
                    .arguments(commandLine.subList(1, commandLine.size()))
                    .output().consumeLinesWith(8192, System.out::println)
                    .error().consumeLinesWith(8192, System.err::println)
                    .run();
            return true;
        } catch (Exception e) {
            if (e instanceof AbnormalExitException ae && ae.exitCode() == 127) {
                log.errorf("Command: %s failed because the builder image does not provide the `upx` executable",
                        String.join(" ", commandLine));
            } else {
                log.errorf(e, "Command: %s failed", String.join(" ", commandLine));
            }
            return false;
        }
    }

    private String getCompressionLevel(int level) {
        if (level == 10) {
            return "--best";
        }
        if (level > 0 && level < 10) {
            return "-" + level;
        }
        throw new IllegalArgumentException("Invalid compression level, " + level + " is not in [1, 10]");
    }

    private Optional<File> getUpxFromSystem() {
        return ProcessUtil.pathOfCommand(Path.of(getUpxExecutableName()))
                .map(Path::toFile);
    }

    private static String getUpxExecutableName() {
        return SystemUtils.IS_OS_WINDOWS ? "upx.exe" : "upx";
    }
}
