package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.UpxCompressedBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.runtime.util.ContainerRuntimeUtil;

public class UpxCompressionBuildStep {

    private static final Logger log = Logger.getLogger(UpxCompressionBuildStep.class);

    /**
     * The name of the environment variable containing the system path.
     */
    private static final String PATH = "PATH";

    @BuildStep(onlyIf = NativeBuild.class)
    public void compress(NativeConfig nativeConfig, NativeImageBuildItem image,
            BuildProducer<UpxCompressedBuildItem> upxCompressedProducer,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {
        if (nativeConfig.compression.level.isEmpty()) {
            log.debug("UPX compression disabled");
            return;
        }

        Optional<File> upxPathFromSystem = getUpxFromSystem();
        if (upxPathFromSystem.isPresent()) {
            log.debug("Running UPX from system path");
            if (!runUpxFromHost(upxPathFromSystem.get(), image.getPath().toFile(), nativeConfig)) {
                throw new IllegalStateException("Unable to compress the native executable");
            }
        } else if (nativeConfig.isContainerBuild()) {
            log.infof("Running UPX from a container using the builder image: " + nativeConfig.getEffectiveBuilderImage());
            if (!runUpxInContainer(image, nativeConfig)) {
                throw new IllegalStateException("Unable to compress the native executable");
            }
        } else {
            log.errorf("Unable to compress the native executable. Either install `upx` from https://upx.github.io/" +
                    " on your machine, or enable in-container build using `-Dquarkus.native.container-build=true`.");
            throw new IllegalStateException("Unable to compress the native executable: `upx` not available");
        }
        log.infof("Native executable compressed: %s", image.getPath().toFile().getAbsolutePath());
        upxCompressedProducer.produce(new UpxCompressedBuildItem());
    }

    private boolean runUpxFromHost(File upx, File executable, NativeConfig nativeConfig) {
        String level = getCompressionLevel(nativeConfig.compression.level.getAsInt());
        List<String> extraArgs = nativeConfig.compression.additionalArgs.orElse(Collections.emptyList());
        List<String> args = Stream.concat(
                Stream.concat(Stream.of(upx.getAbsolutePath(), level), extraArgs.stream()),
                Stream.of(executable.getAbsolutePath()))
                .collect(Collectors.toList());
        log.infof("Executing %s", String.join(" ", args));
        final ProcessBuilder processBuilder = new ProcessBuilder(args)
                .directory(executable.getAbsoluteFile().getParentFile())
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE);
        Process process = null;
        try {
            process = processBuilder.start();
            ProcessUtil.streamOutputToSysOut(process);
            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.errorf("Command: " + String.join(" ", args) + " failed with exit code " + exitCode);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.errorf("Command: " + String.join(" ", args) + " failed", e);
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

    }

    private boolean runUpxInContainer(NativeImageBuildItem nativeImage, NativeConfig nativeConfig) {
        String level = getCompressionLevel(nativeConfig.compression.level.getAsInt());
        List<String> extraArgs = nativeConfig.compression.additionalArgs.orElse(Collections.emptyList());

        List<String> commandLine = new ArrayList<>();
        ContainerRuntimeUtil.ContainerRuntime containerRuntime = nativeConfig.containerRuntime
                .orElseGet(ContainerRuntimeUtil::detectContainerRuntime);
        commandLine.add(containerRuntime.getExecutableName());

        commandLine.add("run");
        commandLine.add("--env");
        commandLine.add("LANG=C");
        commandLine.add("--rm");
        commandLine.add("--entrypoint=upx");

        String containerName = "upx-" + RandomStringUtils.random(5, true, false);
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
                if (containerRuntime == ContainerRuntimeUtil.ContainerRuntime.PODMAN) {
                    // Needed to avoid AccessDeniedExceptions
                    commandLine.add("--userns=keep-id");
                }
            }
        }

        Collections.addAll(commandLine, "-v",
                volumeOutputPath + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + ":z");

        commandLine.add(nativeConfig.getEffectiveBuilderImage());
        commandLine.add(level);
        commandLine.addAll(extraArgs);

        commandLine.add(nativeImage.getPath().toFile().getName());

        log.infof("Compress native executable using: %s", String.join(" ", commandLine));
        final ProcessBuilder processBuilder = new ProcessBuilder(commandLine)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE);
        Process process = null;
        try {
            process = processBuilder.start();
            ProcessUtil.streamOutputToSysOut(process);
            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                if (exitCode == 127) {
                    log.errorf("Command: %s failed because the builder image does not provide the `upx` executable",
                            String.join(" ", commandLine));
                } else {
                    log.errorf("Command: %s failed with exit code %d", String.join(" ", commandLine), exitCode);
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            log.errorf("Command: " + String.join(" ", commandLine) + " failed", e);
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
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
        String exec = getUpxExecutableName();
        String systemPath = System.getenv(PATH);
        if (systemPath != null) {
            String[] pathDirs = systemPath.split(File.pathSeparator);
            for (String pathDir : pathDirs) {
                File dir = new File(pathDir);
                if (dir.isDirectory()) {
                    File file = new File(dir, exec);
                    if (file.exists()) {
                        return Optional.of(file);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static String getUpxExecutableName() {
        return SystemUtils.IS_OS_WINDOWS ? "upx.exe" : "upx";
    }
}
