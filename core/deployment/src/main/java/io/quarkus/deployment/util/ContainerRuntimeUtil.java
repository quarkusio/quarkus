package io.quarkus.deployment.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.smallrye.common.os.OS;
import io.smallrye.common.process.ProcessBuilder;
import io.smallrye.common.process.ProcessUtil;
import io.smallrye.config.SmallRyeConfig;

public final class ContainerRuntimeUtil {

    private static final Logger log = Logger.getLogger(ContainerRuntimeUtil.class);
    private static final String CONTAINER_EXECUTABLE = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
            .getOptionalValue("quarkus.native.container-runtime", String.class).orElse(null);
    private static final Pattern PODMAN_PATTERN = Pattern.compile("^podman(?i:\\.exe)? version.*", Pattern.DOTALL);

    /**
     * Static variable is not used because the class gets loaded by different classloaders at
     * runtime and the container runtime would be detected again and again unnecessarily.
     */
    private static final String CONTAINER_RUNTIME_SYS_PROP = "quarkus-local-container-runtime";
    /**
     * Defines the maximum number of characters to read from the output of the `docker info` command.
     */
    private static final int MAX_ANTICIPATED_CHARACTERS_IN_DOCKER_INFO = 3000;

    private ContainerRuntimeUtil() {
    }

    /**
     * @return a fully resolved {@link ContainerRuntime} indicating if Docker or Podman is available and in rootless mode or not
     * @throws IllegalStateException if no container runtime was found to build the image
     */
    public static ContainerRuntime detectContainerRuntime() {
        return detectContainerRuntime(true);
    }

    public static ContainerRuntime detectContainerRuntime(ContainerRuntime... orderToCheckRuntimes) {
        return detectContainerRuntime(true, orderToCheckRuntimes);
    }

    public static ContainerRuntime detectContainerRuntime(boolean required, ContainerRuntime... orderToCheckRuntimes) {
        return detectContainerRuntime(required,
                ((orderToCheckRuntimes != null) && (orderToCheckRuntimes.length > 0)) ? Arrays.asList(orderToCheckRuntimes)
                        : List.of(ContainerRuntime.DOCKER, ContainerRuntime.PODMAN));
    }

    public static ContainerRuntime detectContainerRuntime(boolean required, boolean silent,
            ContainerRuntime... orderToCheckRuntimes) {
        return detectContainerRuntime(required, silent,
                ((orderToCheckRuntimes != null) && (orderToCheckRuntimes.length > 0)) ? Arrays.asList(orderToCheckRuntimes)
                        : List.of(ContainerRuntime.DOCKER, ContainerRuntime.PODMAN));
    }

    public static ContainerRuntime detectContainerRuntime(boolean required, List<ContainerRuntime> orderToCheckRuntimes) {
        return detectContainerRuntime(required, false, orderToCheckRuntimes);
    }

    public static ContainerRuntime detectContainerRuntime(boolean required, boolean silent,
            List<ContainerRuntime> orderToCheckRuntimes) {
        ContainerRuntime containerRuntime = loadContainerRuntimeFromSystemProperty();
        if ((containerRuntime != null) && orderToCheckRuntimes.contains(containerRuntime)) {
            return containerRuntime;
        }

        final ContainerRuntime containerRuntimeEnvironment = getContainerRuntimeEnvironment(orderToCheckRuntimes);
        if (containerRuntimeEnvironment == ContainerRuntime.UNAVAILABLE) {
            storeContainerRuntimeInSystemProperty(ContainerRuntime.UNAVAILABLE);

            if (required) {
                throw new IllegalStateException("No container CLI was found. "
                        + "Make sure you have either Docker or Podman CLI installed in your environment.");
            }

            return ContainerRuntime.UNAVAILABLE;
        }

        // we have a working container environment, let's resolve it fully
        containerRuntime = fullyResolveContainerRuntime(containerRuntimeEnvironment, silent);

        storeContainerRuntimeInSystemProperty(containerRuntime);

        return containerRuntime;
    }

    private static ContainerRuntime getContainerRuntimeEnvironment(List<ContainerRuntime> orderToCheckRuntimes) {
        // Docker version 19.03.14, build 5eb3275d40

        // Check if Podman is installed
        // podman version 2.1.1
        var runtimesToCheck = new ArrayList<>(orderToCheckRuntimes.stream().distinct().toList());
        runtimesToCheck.retainAll(List.of(ContainerRuntime.DOCKER, ContainerRuntime.PODMAN));

        if (CONTAINER_EXECUTABLE != null) {
            var runtime = runtimesToCheck.stream().filter(
                    containerRuntime -> CONTAINER_EXECUTABLE.trim().equalsIgnoreCase(containerRuntime.getExecutableName()))
                    .findFirst().filter(r -> {
                        var versionOutput = getVersionOutputFor(r);

                        return switch (r) {
                            case DOCKER, DOCKER_ROOTLESS -> versionOutput.contains("Docker version");
                            case PODMAN, PODMAN_ROOTLESS -> PODMAN_PATTERN.matcher(versionOutput).matches();
                            default -> false;
                        };
                    });

            if (runtime.isPresent()) {
                return runtime.get();
            } else {
                log.warn("quarkus.native.container-runtime config property must be set to either podman or docker "
                        + "and the executable must be available. Ignoring it.");
            }
        }

        for (var runtime : runtimesToCheck) {
            var versionOutput = getVersionOutputFor(runtime);

            switch (runtime) {
                case DOCKER:
                case DOCKER_ROOTLESS:
                    var dockerAvailable = versionOutput.contains("Docker version");
                    if (dockerAvailable) {
                        // Check if "docker" is an alias to podman
                        return PODMAN_PATTERN.matcher(versionOutput).matches() ? ContainerRuntime.PODMAN
                                : ContainerRuntime.DOCKER;
                    }
                    break;

                case PODMAN:
                case PODMAN_ROOTLESS:
                    if (PODMAN_PATTERN.matcher(versionOutput).matches()) {
                        return ContainerRuntime.PODMAN;
                    }
                    break;
            }
        }

        return ContainerRuntime.UNAVAILABLE;
    }

    private static ContainerRuntime fullyResolveContainerRuntime(ContainerRuntime containerRuntimeEnvironment,
            boolean silent) {
        String execName = containerRuntimeEnvironment.getExecutableName();
        try {
            Optional<Path> execPath = ProcessUtil.pathOfCommand(Path.of(execName));
            if (execPath.isEmpty()) {
                // this should never happen as we have detected the presence of the Docker/Podman CLI before
                throw new IllegalStateException(
                        String.format("Unable to find command: %s in $PATH: %s", execName, ProcessUtil.searchPath()));
            }

            return ProcessBuilder.newBuilder(execPath.get())
                    .arguments("info")
                    .output().gatherOnFail(true).processWith(br -> {
                        boolean rootless = false;
                        boolean isInWindowsWSL = false;
                        boolean isDocker = containerRuntimeEnvironment.isDocker();
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (isDocker) {
                                if (line.trim().equals("rootless") || line.contains("Docker Desktop")
                                        || line.contains("desktop-linux")) {
                                    rootless = true;
                                }
                            } else {
                                if (line.trim().equals("rootless: true")) {
                                    rootless = true;
                                }
                            }
                            if (OS.current() == OS.LINUX && isDocker) {
                                if (line.trim().contains("WSL")) {
                                    isInWindowsWSL = true;
                                }
                            }
                        }
                        if (rootless) {
                            if (isInWindowsWSL) {
                                return ContainerRuntime.WSL_ROOTLESS;
                            }
                            return containerRuntimeEnvironment == ContainerRuntime.DOCKER ? ContainerRuntime.DOCKER_ROOTLESS
                                    : ContainerRuntime.PODMAN_ROOTLESS;
                        }

                        if (isInWindowsWSL) {
                            return ContainerRuntime.WSL;
                        }

                        return containerRuntimeEnvironment;
                    })
                    .error().logOnSuccess(false)
                    .run();
        } catch (Exception e) {
            if (!silent) {
                log.warnf("Command \"%s\" failed. "
                        + "Rootless container runtime detection might not be reliable or the container service is not running at all.",
                        execName);
            }
            return ContainerRuntime.UNAVAILABLE;
        }
    }

    private static ContainerRuntime loadContainerRuntimeFromSystemProperty() {
        final String runtime = System.getProperty(CONTAINER_RUNTIME_SYS_PROP);

        if (runtime == null) {
            return null;
        }

        final ContainerRuntime containerRuntime = ContainerRuntime.of(runtime);

        if (containerRuntime == null) {
            log.warnf("System property %s contains an unknown value %s. Ignoring it.", CONTAINER_RUNTIME_SYS_PROP, runtime);
        }

        return containerRuntime;
    }

    private static void storeContainerRuntimeInSystemProperty(ContainerRuntime containerRuntime) {
        System.setProperty(CONTAINER_RUNTIME_SYS_PROP, containerRuntime.name());
    }

    private static String getVersionOutputFor(ContainerRuntime containerRuntime) {
        String execName = containerRuntime.getExecutableName();
        Optional<Path> execPath = ProcessUtil.pathOfCommand(Path.of(execName));
        if (execPath.isEmpty()) {
            log.debugf("Unable to find command %s in $PATH: %s", execName, ProcessUtil.searchPath());
            return "";
        }

        try {
            return ProcessBuilder.newBuilder(execPath.get())
                    .arguments("--version")
                    .output().gatherOnFail(true).toSingleString(16384)
                    .error().logOnSuccess(false)
                    .run();
        } catch (Throwable t) {
            // If an exception is thrown in the process, just return an empty String
            log.debugf(t, "Failure to read version output from %s", execPath.get());
            return "";
        }
    }

    /**
     * Supported Container runtimes
     */
    public enum ContainerRuntime {
        DOCKER("docker", false),
        DOCKER_ROOTLESS("docker", true),
        WSL("docker", false),
        WSL_ROOTLESS("docker", false),
        PODMAN("podman", false),
        PODMAN_ROOTLESS("podman", true),
        UNAVAILABLE(null, false);

        private final String executableName;

        private final boolean rootless;

        ContainerRuntime(String executableName, boolean rootless) {
            this.executableName = executableName;
            this.rootless = rootless;
        }

        public String getExecutableName() {
            if (this == UNAVAILABLE) {
                throw new IllegalStateException("Cannot get an executable name when no container runtime is available");
            }

            return executableName;
        }

        public boolean isDocker() {
            return this == DOCKER || this == DOCKER_ROOTLESS || this == WSL || this == WSL_ROOTLESS;
        }

        public boolean isPodman() {
            return this == PODMAN || this == PODMAN_ROOTLESS;
        }

        public boolean isInWindowsWSL() {
            return this == WSL || this == WSL_ROOTLESS;
        }

        public boolean isRootless() {
            return rootless;
        }

        public boolean isUnavailable() {
            return this == UNAVAILABLE;
        }

        public static ContainerRuntime of(String value) {
            for (ContainerRuntime containerRuntime : values()) {
                if (containerRuntime.name().equalsIgnoreCase(value)) {
                    return containerRuntime;
                }
            }

            return null;
        }
    }
}
