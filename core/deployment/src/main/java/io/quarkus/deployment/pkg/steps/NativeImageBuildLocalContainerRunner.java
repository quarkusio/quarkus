package io.quarkus.deployment.pkg.steps;

import static io.quarkus.deployment.pkg.steps.LinuxIDUtil.getLinuxID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.OutputFilter;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.util.ExecUtil;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.util.ContainerRuntimeUtil;

public class NativeImageBuildLocalContainerRunner extends NativeImageBuildContainerRunner {

    private static final Logger LOGGER = Logger.getLogger(NativeImageBuildLocalContainerRunner.class.getName());

    public NativeImageBuildLocalContainerRunner(NativeConfig nativeConfig, Path outputDir) {
        super(nativeConfig, outputDir);
        if (SystemUtils.IS_OS_LINUX) {
            ArrayList<String> containerRuntimeArgs = new ArrayList<>(Arrays.asList(baseContainerRuntimeArgs));
            if (isDockerRootless(containerRuntime)) {
                Collections.addAll(containerRuntimeArgs, "--user", String.valueOf(0));
            } else {
                String uid = getLinuxID("-ur");
                String gid = getLinuxID("-gr");
                if (uid != null && gid != null && !uid.isEmpty() && !gid.isEmpty()) {
                    Collections.addAll(containerRuntimeArgs, "--user", uid + ":" + gid);
                    if (containerRuntime == ContainerRuntimeUtil.ContainerRuntime.PODMAN) {
                        // Needed to avoid AccessDeniedExceptions
                        containerRuntimeArgs.add("--userns=keep-id");
                    }
                }
            }
            baseContainerRuntimeArgs = containerRuntimeArgs.toArray(baseContainerRuntimeArgs);
        }
    }

    private static boolean isDockerRootless(ContainerRuntimeUtil.ContainerRuntime containerRuntime) {
        if (containerRuntime != ContainerRuntimeUtil.ContainerRuntime.DOCKER) {
            return false;
        }
        String dockerEndpoint = fetchDockerEndpoint();
        // docker socket?
        String socketUriPrefix = "unix://";
        if (dockerEndpoint == null || !dockerEndpoint.startsWith(socketUriPrefix)) {
            return false;
        }
        String dockerSocket = dockerEndpoint.substring(socketUriPrefix.length());
        String currentUid = getLinuxID("-ur");
        if (currentUid == null || currentUid.isEmpty() || currentUid.equals(String.valueOf(0))) {
            return false;
        }

        int socketOwnerUid;
        try {
            socketOwnerUid = (int) Files.getAttribute(Path.of(dockerSocket), "unix:uid", LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            LOGGER.infof("Owner UID lookup on '%s' failed with '%s'", dockerSocket, e.getMessage());
            return false;
        }
        return currentUid.equals(String.valueOf(socketOwnerUid));
    }

    private static String fetchDockerEndpoint() {
        // DOCKER_HOST environment variable overrides the active context
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null) {
            return dockerHost;
        }

        OutputFilter outputFilter = new OutputFilter();
        if (!ExecUtil.execWithTimeout(new File("."), outputFilter, Duration.ofMillis(3000),
                "docker", "context", "ls", "--format",
                "{{- if .Current -}} {{- .DockerEndpoint -}} {{- end -}}")) {
            LOGGER.debug("Docker context lookup didn't succeed in time");
            return null;
        }

        Set<String> endpoints = outputFilter.getOutput().lines()
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toSet());
        if (endpoints.size() == 1) {
            return endpoints.stream().findFirst().orElse(null);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugf("Found too many active Docker endpoints: [%s]",
                    endpoints.stream()
                            .map(endpoint -> String.format("'%s'", endpoint))
                            .collect(Collectors.joining(",")));
        }
        return null;
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
