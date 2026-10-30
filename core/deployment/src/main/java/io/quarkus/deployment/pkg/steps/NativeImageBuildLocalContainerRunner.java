package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.deployment.util.FileUtil;
import io.smallrye.common.process.ProcessBuilder;

public class NativeImageBuildLocalContainerRunner extends NativeImageBuildContainerRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildLocalContainerRunner.class);

    private volatile Thread cleanupHook;

    public NativeImageBuildLocalContainerRunner(NativeConfig nativeConfig) {
        super(nativeConfig);
        List<String> containerRuntimeArgs = new ArrayList<>(Arrays.asList(baseContainerRuntimeArgs));
        if (SystemUtils.IS_OS_LINUX && containerRuntime.isInWindowsWSL()) {
            containerRuntimeArgs.add("--interactive");
        }
        containerRuntimeArgs
                .addAll(getVolumeAccessArguments(containerRuntime, nativeConfig.builderImage().getEffectiveImage()));
        baseContainerRuntimeArgs = containerRuntimeArgs.toArray(baseContainerRuntimeArgs);
    }

    /**
     * Runs the build container without {@code --rm} so that, on failure, its {@code State.OOMKilled} flag can be
     * inspected before the container is removed (see {@link #postBuild}). The container is removed by Quarkus
     * instead, both on completion and via a shutdown hook if the build is interrupted. See
     * <a href="https://github.com/quarkusio/quarkus/issues/1140">#1140</a>.
     */
    @Override
    protected String[] getBuildCommand(Path outputDir, List<String> args) {
        return Arrays.stream(super.getBuildCommand(outputDir, args))
                .filter(arg -> !"--rm".equals(arg))
                .toArray(String[]::new);
    }

    @Override
    protected void preBuild(Path outputDir, List<String> buildArgs) throws IOException, InterruptedException {
        cleanupHook = new Thread(this::removeBuildContainer, "native-image-build-container-cleanup");
        Runtime.getRuntime().addShutdownHook(cleanupHook);
        super.preBuild(outputDir, buildArgs);
    }

    @Override
    protected void postBuild(Path outputDir, String nativeImageName, String resultingExecutableName)
            throws InterruptedException, IOException {
        try {
            if (wasOutOfMemoryKilled()) {
                log.error("The native image build container was killed by the out-of-memory killer "
                        + "(the container runtime reports State.OOMKilled=true). Give the build more memory - e.g. raise "
                        + "\"quarkus.native.native-image-xmx\", or increase the memory available to the container runtime.");
            }
        } finally {
            removeBuildContainer();
            if (cleanupHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(cleanupHook);
                } catch (IllegalStateException ignored) {
                    // JVM is already shutting down; the hook will run (or has run) on its own.
                }
                cleanupHook = null;
            }
        }
        super.postBuild(outputDir, nativeImageName, resultingExecutableName);
    }

    private boolean wasOutOfMemoryKilled() {
        try {
            List<String> output = ProcessBuilder.newBuilder(containerRuntime.getExecutableName())
                    .arguments("inspect", "--format", "{{.State.OOMKilled}}", containerName)
                    .output().toStringList(1024, 256)
                    .error().discard()
                    .exitCodeChecker(ec -> true)
                    .run();
            return output.stream().anyMatch(line -> "true".equals(line.trim()));
        } catch (Exception e) {
            log.debugf(e, "Could not inspect the native image build container for an out-of-memory kill");
            return false;
        }
    }

    private void removeBuildContainer() {
        runCommand(new String[] { containerRuntime.getExecutableName(), "rm", "-f", containerName }, null);
    }

    /**
     * Returns the container runtime arguments needed to ensure that files written to a bind-mounted
     * volume on the host are owned by the real host user.
     * <p>
     * The UID/GID of the running user inside {@code image} is detected dynamically via
     * {@link ContainerUserResolver} so that any container image works correctly, regardless of
     * which user it declares. When detection fails, Podman falls back to {@code --user} with the
     * host UID/GID (and {@code --userns=keep-id} for rootless Podman).
     *
     * @param containerRuntime the detected container runtime
     * @param image the container image that will be run (used for UID/GID detection)
     * @return the list of extra {@code run} arguments, or an empty list when not applicable
     */
    public static List<String> getVolumeAccessArguments(ContainerRuntime containerRuntime, String image) {
        if (containerRuntime.isUnavailable()) {
            return List.of();
        }

        final List<String> result = new ArrayList<>();
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            if (containerRuntime.isRootless()) {
                if (containerRuntime.isDocker()) {
                    Collections.addAll(result, "--user", String.valueOf(0));
                } else if (containerRuntime.isPodman()) {
                    // Rootless Podman: map the container user to the host user inside the user namespace.
                    // The UID/GID is resolved dynamically so any builder image works correctly.
                    ContainerUserResolver.ContainerUser containerUser = ContainerUserResolver.resolve(containerRuntime, image);
                    if (containerUser == null) {
                        String uid = getLinuxID("-ur");
                        String gid = getLinuxID("-gr");
                        if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                            Collections.addAll(result, "--user", uid + ":" + gid);
                            result.add("--userns=keep-id");
                        } else {
                            log.warn(
                                    "Cannot determine host UID/GID; rootless Podman volume permissions may be incorrect.");
                        }
                    } else {
                        result.add("--userns=keep-id:uid=" + containerUser.uid() + ",gid=" + containerUser.gid());
                    }
                }
            } else {
                String uid = getLinuxID("-ur");
                String gid = getLinuxID("-gr");
                if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                    if (containerRuntime.isPodman()) {
                        // Rootful Podman: remap the container user's UID/GID to the host user's UID/GID.
                        ContainerUserResolver.ContainerUser containerUser = ContainerUserResolver.resolve(containerRuntime,
                                image);
                        if (containerUser == null) {
                            Collections.addAll(result, "--user", uid + ":" + gid);
                        } else {
                            // Map system IDs (including root) so bind-mounted files remain accessible
                            Collections.addAll(result, "--uidmap", "0:0:999", "--gidmap", "0:0:999");
                            Collections.addAll(result, "--uidmap", containerUser.uid() + ":" + uid + ":1");
                            Collections.addAll(result, "--gidmap", containerUser.gid() + ":" + gid + ":1");
                        }
                    } else {
                        Collections.addAll(result, "--user", uid + ":" + gid);
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected List<String> getContainerRuntimeBuildArgs(Path outputDir) {
        final List<String> containerRuntimeArgs = super.getContainerRuntimeBuildArgs(outputDir);
        String volumeOutputPath = outputDir.toAbsolutePath().toString();
        addVolumeParameter(volumeOutputPath, NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH, containerRuntimeArgs,
                containerRuntime);
        return containerRuntimeArgs;
    }

    public static void addVolumeParameter(String localPath, String remotePath, List<String> args,
            ContainerRuntime containerRuntime) {
        if (SystemUtils.IS_OS_WINDOWS) {
            localPath = FileUtil.translateToVolumePath(localPath);
        }

        final String selinuxBindOption;
        if (SystemUtils.IS_OS_MAC && containerRuntime.isPodman()) {
            selinuxBindOption = "";
        } else {
            selinuxBindOption = ":z";
        }

        args.add("-v");
        args.add(localPath + ":" + remotePath + selinuxBindOption);
    }
}
