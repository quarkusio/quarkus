package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.ContainerRuntimeUtil.ContainerRuntime;
import io.quarkus.deployment.util.FileUtil;

public class NativeImageBuildLocalContainerRunner extends NativeImageBuildContainerRunner {

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
     * Returns the container runtime arguments needed to ensure that files written to a bind-mounted
     * volume on the host are owned by the real host user.
     * <p>
     * The UID/GID of the running user inside {@code image} is detected dynamically via
     * {@link ContainerUserResolver} so that any container image works correctly, regardless of
     * which user it declares.
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
            if (containerRuntime.isDocker() && containerRuntime.isRootless()) {
                Collections.addAll(result, "--user", String.valueOf(0));
            } else if (containerRuntime.isPodman() && containerRuntime.isRootless()) {
                // Rootless Podman: map the container user to the host user inside the user namespace.
                // The UID/GID is resolved dynamically so any builder image works correctly.
                ContainerUserResolver.ContainerUser containerUser = ContainerUserResolver.resolve(containerRuntime, image);
                if (containerUser == null) {
                    throw new RuntimeException(
                            "Cannot determine the UID/GID of the user inside container image '" + image + "'. " +
                                    "Ensure the image is available locally and contains the 'id' command.");
                }
                result.add("--userns=keep-id:uid=" + containerUser.uid() + ",gid=" + containerUser.gid());
            } else {
                String uid = getLinuxID("-ur");
                String gid = getLinuxID("-gr");
                if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                    if (containerRuntime.isPodman()) {
                        // Rootful Podman: remap the container user's UID/GID to the host user's UID/GID.
                        ContainerUserResolver.ContainerUser containerUser = ContainerUserResolver.resolve(containerRuntime,
                                image);
                        if (containerUser == null) {
                            throw new RuntimeException(
                                    "Cannot determine the UID/GID of the user inside container image '" + image + "'. " +
                                            "Ensure the image is available locally and contains the 'id' command.");
                        }
                        Collections.addAll(result, "--uidmap", containerUser.uid() + ":" + uid + ":1");
                        Collections.addAll(result, "--gidmap", containerUser.gid() + ":" + gid + ":1");
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
