package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.util.ContainerRuntimeUtil;

public class NativeImageBuildLocalContainerRunner extends NativeImageBuildContainerRunner {

    private static final Logger LOGGER = Logger.getLogger(NativeImageBuildLocalContainerRunner.class.getName());

    public NativeImageBuildLocalContainerRunner(NativeConfig nativeConfig, Path outputDir) {
        super(nativeConfig, outputDir);
        if (SystemUtils.IS_OS_LINUX) {
            ArrayList<String> containerRuntimeArgs = new ArrayList<>(Arrays.asList(baseContainerRuntimeArgs));
            if (containerRuntime == ContainerRuntimeUtil.ContainerRuntime.DOCKER
                    && containerRuntime.isRootless()) {
                Collections.addAll(containerRuntimeArgs, "--user", String.valueOf(0));
            } else {
                String uid = getLinuxID("-ur");
                String gid = getLinuxID("-gr");
                if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                    Collections.addAll(containerRuntimeArgs, "--user", uid + ":" + gid);
                    if (containerRuntime == ContainerRuntimeUtil.ContainerRuntime.PODMAN
                            && containerRuntime.isRootless()) {
                        // Needed to avoid AccessDeniedExceptions
                        containerRuntimeArgs.add("--userns=keep-id");
                    }
                }
            }
            baseContainerRuntimeArgs = containerRuntimeArgs.toArray(baseContainerRuntimeArgs);
        }
    }

    @Override
    protected List<String> getContainerRuntimeBuildArgs() {
        List<String> containerRuntimeArgs = super.getContainerRuntimeBuildArgs();
        String volumeOutputPath = outputPath;
        if (SystemUtils.IS_OS_WINDOWS) {
            volumeOutputPath = FileUtil.translateToVolumePath(volumeOutputPath);
        }

        String selinuxBindOption = ":z";
        if (SystemUtils.IS_OS_MAC
                && ContainerRuntimeUtil.detectContainerRuntime() == ContainerRuntimeUtil.ContainerRuntime.PODMAN) {
            selinuxBindOption = "";
        }

        Collections.addAll(containerRuntimeArgs, "-v",
                volumeOutputPath + ":" + NativeImageBuildStep.CONTAINER_BUILD_VOLUME_PATH + selinuxBindOption);
        return containerRuntimeArgs;
    }

}
