package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.FileUtil;

public class NativeImageBuildLocalContainerRunner extends NativeImageBuildContainerRunner {

    public NativeImageBuildLocalContainerRunner(NativeConfig nativeConfig) {
        super(nativeConfig);
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            final ArrayList<String> containerRuntimeArgs = new ArrayList<>(Arrays.asList(baseContainerRuntimeArgs));
            if (containerRuntime.isInWindowsWSL()) {
                containerRuntimeArgs.add("--interactive");
            }
            if (containerRuntime.isDocker() && containerRuntime.isRootless()) {
                Collections.addAll(containerRuntimeArgs, "--user", String.valueOf(0));
            } else {
                String uid = getLinuxID("-ur");
                String gid = getLinuxID("-gr");
                if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                    Collections.addAll(containerRuntimeArgs, "--user", uid + ":" + gid);
                    if (containerRuntime.isPodman() && containerRuntime.isRootless()) {
                        // Needed to avoid AccessDeniedExceptions
                        containerRuntimeArgs.add("--userns=keep-id");
                    }
                }
            }
            baseContainerRuntimeArgs = containerRuntimeArgs.toArray(baseContainerRuntimeArgs);
        }
    }

    @Override
    protected List<String> getContainerRuntimeBuildArgs(Path outputDir) {
        final List<String> containerRuntimeArgs = super.getContainerRuntimeBuildArgs(outputDir);
        String volumeOutputPath = outputDir.toAbsolutePath().toString();
        if (SystemUtils.IS_OS_WINDOWS) {
            volumeOutputPath = FileUtil.translateToVolumePath(volumeOutputPath);
        }

        final String selinuxBindOption;
        if (SystemUtils.IS_OS_MAC && containerRuntime.isPodman()) {
            selinuxBindOption = "";
        } else {
            selinuxBindOption = ":z";
        }

        Collections.addAll(containerRuntimeArgs, "-v",
                volumeOutputPath + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + selinuxBindOption);
        return containerRuntimeArgs;
    }

}
