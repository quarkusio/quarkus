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
        containerRuntimeArgs.addAll(getVolumeAccessArguments(containerRuntime));
        baseContainerRuntimeArgs = containerRuntimeArgs.toArray(baseContainerRuntimeArgs);
    }

    public static List<String> getVolumeAccessArguments(ContainerRuntime containerRuntime) {
        final List<String> result = new ArrayList<>();
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            if (containerRuntime.isDocker() && containerRuntime.isRootless()) {
                Collections.addAll(result, "--user", String.valueOf(0));
            } else {
                String uid = getLinuxID("-ur");
                String gid = getLinuxID("-gr");
                if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                    Collections.addAll(result, "--user", uid + ":" + gid);
                    if (containerRuntime.isPodman() && containerRuntime.isRootless()) {
                        // Needed to avoid AccessDeniedExceptions
                        result.add("--userns=keep-id");
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
