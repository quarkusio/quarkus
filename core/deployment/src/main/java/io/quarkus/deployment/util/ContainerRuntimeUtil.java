package io.quarkus.deployment.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.smallrye.config.SmallRyeConfig;

public final class ContainerRuntimeUtil {

    private static final Logger log = Logger.getLogger(ContainerRuntimeUtil.class);
    private static final String CONTAINER_EXECUTABLE = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
            .getOptionalValue("quarkus.native.container-runtime", String.class).orElse(null);
    private static final Pattern PODMAN_PATTERN = Pattern.compile("^podman(?:\\.exe)? version.*", Pattern.DOTALL);

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

    public static ContainerRuntime detectContainerRuntime(boolean required) {
        ContainerRuntime containerRuntime = loadContainerRuntimeFromSystemProperty();
        if (containerRuntime != null) {
            return containerRuntime;
        }

        final ContainerRuntime containerRuntimeEnvironment = getContainerRuntimeEnvironment();
        if (containerRuntimeEnvironment == ContainerRuntime.UNAVAILABLE) {
            storeContainerRuntimeInSystemProperty(ContainerRuntime.UNAVAILABLE);

            if (required) {
                throw new IllegalStateException("No container runtime was found. "
                        + "Make sure you have either Docker or Podman installed in your environment.");
            }

            return ContainerRuntime.UNAVAILABLE;
        }

        // we have a working container environment, let's resolve it fully
        containerRuntime = fullyResolveContainerRuntime(containerRuntimeEnvironment);

        storeContainerRuntimeInSystemProperty(containerRuntime);

        return containerRuntime;
    }

    private static ContainerRuntime getContainerRuntimeEnvironment() {
        // Docker version 19.03.14, build 5eb3275d40
        String dockerVersionOutput;
        boolean dockerAvailable;
        // Check if Podman is installed
        // podman version 2.1.1
        String podmanVersionOutput;
        boolean podmanAvailable;

        if (CONTAINER_EXECUTABLE != null) {
            if (CONTAINER_EXECUTABLE.trim().equalsIgnoreCase("docker")) {
                dockerVersionOutput = getVersionOutputFor(ContainerRuntime.DOCKER);
                dockerAvailable = dockerVersionOutput.contains("Docker version");
                if (dockerAvailable) {
                    return ContainerRuntime.DOCKER;
                }
            }
            if (CONTAINER_EXECUTABLE.trim().equalsIgnoreCase("podman")) {
                podmanVersionOutput = getVersionOutputFor(ContainerRuntime.PODMAN);
                podmanAvailable = PODMAN_PATTERN.matcher(podmanVersionOutput).matches();
                if (podmanAvailable) {
                    return ContainerRuntime.PODMAN;
                }
            }
            log.warn("quarkus.native.container-runtime config property must be set to either podman or docker " +
                    "and the executable must be available. Ignoring it.");
        }

        dockerVersionOutput = getVersionOutputFor(ContainerRuntime.DOCKER);
        dockerAvailable = dockerVersionOutput.contains("Docker version");
        if (dockerAvailable) {
            // Check if "docker" is an alias to "podman"
            if (PODMAN_PATTERN.matcher(dockerVersionOutput).matches()) {
                return ContainerRuntime.PODMAN;
            }
            return ContainerRuntime.DOCKER;
        }
        podmanVersionOutput = getVersionOutputFor(ContainerRuntime.PODMAN);
        podmanAvailable = PODMAN_PATTERN.matcher(podmanVersionOutput).matches();
        if (podmanAvailable) {
            return ContainerRuntime.PODMAN;
        }

        return ContainerRuntime.UNAVAILABLE;
    }

    private static ContainerRuntime fullyResolveContainerRuntime(ContainerRuntime containerRuntimeEnvironment) {
        boolean rootless = false;
        boolean isInWindowsWSL = false;
        Process rootlessProcess = null;
        ProcessBuilder pb = null;
        try {
            pb = new ProcessBuilder(containerRuntimeEnvironment.getExecutableName(), "info").redirectErrorStream(true);
            rootlessProcess = pb.start();
            int exitCode = rootlessProcess.waitFor();
            if (exitCode != 0) {
                log.warnf("Command \"%s\" exited with error code %d. " +
                        "Rootless container runtime detection might not be reliable or the container service is not running at all.",
                        String.join(" ", pb.command()), exitCode);
            }
            try (InputStream inputStream = rootlessProcess.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                bufferedReader.mark(MAX_ANTICIPATED_CHARACTERS_IN_DOCKER_INFO);
                if (exitCode != 0) {
                    log.debugf("Command \"%s\" output: %s", String.join(" ", pb.command()),
                            bufferedReader.lines().collect(Collectors.joining(System.lineSeparator())));
                } else {
                    Predicate<String> stringPredicate;
                    // Docker includes just "rootless" under SecurityOptions, while podman includes "rootless: <boolean>"
                    if (containerRuntimeEnvironment.isDocker()) {
                        // We also treat Docker Desktop as "rootless" since the way it binds mounts does not
                        // transparently map the host user ID and GID
                        // see https://docs.docker.com/desktop/faqs/linuxfaqs/#how-do-i-enable-file-sharing
                        stringPredicate = line -> line.trim().equals("rootless") || line.contains("Docker Desktop")
                                || line.contains("desktop-linux");
                    } else {
                        stringPredicate = line -> line.trim().equals("rootless: true");
                    }
                    rootless = bufferedReader.lines().anyMatch(stringPredicate);

                    if (SystemUtils.IS_OS_LINUX && containerRuntimeEnvironment.isDocker()) {
                        stringPredicate = line -> line.trim().contains("WSL");
                        bufferedReader.reset();
                        isInWindowsWSL = bufferedReader.lines().anyMatch(stringPredicate);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            // If an exception is thrown in the process, assume we are not running rootless (default docker installation)
            log.debugf(e, "Failure to read info output from %s", String.join(" ", pb.command()));
        } finally {
            if (rootlessProcess != null) {
                rootlessProcess.destroy();
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
    }

    private static ContainerRuntime loadContainerRuntimeFromSystemProperty() {
        final String runtime = System.getProperty(CONTAINER_RUNTIME_SYS_PROP);

        if (runtime == null) {
            return null;
        }

        final ContainerRuntime containerRuntime = ContainerRuntime.of(runtime);

        if (containerRuntime == null) {
            log.warnf("System property %s contains an unknown value %s. Ignoring it.",
                    CONTAINER_RUNTIME_SYS_PROP, runtime);
        }

        return containerRuntime;
    }

    private static void storeContainerRuntimeInSystemProperty(ContainerRuntime containerRuntime) {
        System.setProperty(CONTAINER_RUNTIME_SYS_PROP, containerRuntime.name());
    }

    private static String getVersionOutputFor(ContainerRuntime containerRuntime) {
        Process versionProcess = null;
        try {
            final ProcessBuilder pb = new ProcessBuilder(containerRuntime.getExecutableName(), "--version")
                    .redirectErrorStream(true);
            versionProcess = pb.start();
            final int timeoutS = 10;
            if (versionProcess.waitFor(timeoutS, TimeUnit.SECONDS)) {
                return new String(versionProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                log.debugf("Failure. It took command %s more than %d seconds to execute.", containerRuntime.getExecutableName(),
                        timeoutS);
                return "";
            }
        } catch (IOException | InterruptedException e) {
            // If an exception is thrown in the process, just return an empty String
            log.debugf(e, "Failure to read version output from %s", containerRuntime.getExecutableName());
            return "";
        } finally {
            if (versionProcess != null) {
                versionProcess.destroy();
            }
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
            return this.executableName.equals("docker");
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
