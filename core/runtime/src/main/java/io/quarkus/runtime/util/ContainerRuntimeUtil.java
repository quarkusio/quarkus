package io.quarkus.runtime.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.smallrye.config.SmallRyeConfig;

public final class ContainerRuntimeUtil {

    private static final Logger log = Logger.getLogger(ContainerRuntimeUtil.class);
    private static final String DOCKER_EXECUTABLE = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
            .getOptionalValue("quarkus.native.container-runtime", String.class).orElse(null);

    /**
     * Static variable is not used because the class gets loaded by different classloaders at
     * runtime and the container runtime would be detected again and again unnecessarily.
     */
    private static final String CONTAINER_RUNTIME_SYS_PROP = "quarkus-local-container-runtime";

    private ContainerRuntimeUtil() {
    }

    /**
     * @return {@link ContainerRuntime#DOCKER} if it's available, or {@link ContainerRuntime#PODMAN}
     *         if the podman
     *         executable exists in the environment or if the docker executable is an alias to podman,
     *         or {@link ContainerRuntime#UNAVAILABLE} if no container runtime is available and the required arg is false.
     * @throws IllegalStateException if no container runtime was found to build the image
     */
    public static ContainerRuntime detectContainerRuntime() {
        return detectContainerRuntime(true);
    }

    public static ContainerRuntime detectContainerRuntime(boolean required) {
        final ContainerRuntime containerRuntime = loadConfig();
        if (containerRuntime != null) {
            return containerRuntime;
        } else {
            // Docker version 19.03.14, build 5eb3275d40
            final String dockerVersionOutput = getVersionOutputFor(ContainerRuntime.DOCKER);
            boolean dockerAvailable = dockerVersionOutput.contains("Docker version");
            // Check if Podman is installed
            // podman version 2.1.1
            final String podmanVersionOutput = getVersionOutputFor(ContainerRuntime.PODMAN);
            boolean podmanAvailable = podmanVersionOutput.startsWith("podman version");
            if (DOCKER_EXECUTABLE != null) {
                if (DOCKER_EXECUTABLE.trim().equalsIgnoreCase("docker") && dockerAvailable) {
                    storeConfig(ContainerRuntime.DOCKER);
                    return ContainerRuntime.DOCKER;
                } else if (DOCKER_EXECUTABLE.trim().equalsIgnoreCase("podman") && podmanAvailable) {
                    storeConfig(ContainerRuntime.PODMAN);
                    return ContainerRuntime.PODMAN;
                } else {
                    log.warn("quarkus.native.container-runtime config property must be set to either podman or docker " +
                            "and the executable must be available. Ignoring it.");
                }
            }
            if (dockerAvailable) {
                // Check if "docker" is an alias to "podman"
                if (dockerVersionOutput.equals(podmanVersionOutput)) {
                    storeConfig(ContainerRuntime.PODMAN);
                    return ContainerRuntime.PODMAN;
                }
                storeConfig(ContainerRuntime.DOCKER);
                return ContainerRuntime.DOCKER;
            } else if (podmanAvailable) {
                storeConfig(ContainerRuntime.PODMAN);
                return ContainerRuntime.PODMAN;
            } else {
                if (required) {
                    throw new IllegalStateException("No container runtime was found. "
                            + "Make sure you have either Docker or Podman installed in your environment.");
                } else {
                    storeConfig(ContainerRuntime.UNAVAILABLE);
                    return ContainerRuntime.UNAVAILABLE;
                }
            }
        }
    }

    private static ContainerRuntime loadConfig() {
        final String runtime = System.getProperty(CONTAINER_RUNTIME_SYS_PROP);
        if (runtime == null) {
            return null;
        } else if (ContainerRuntime.DOCKER.name().equalsIgnoreCase(runtime)) {
            return ContainerRuntime.DOCKER;
        } else if (ContainerRuntime.PODMAN.name().equalsIgnoreCase(runtime)) {
            return ContainerRuntime.PODMAN;
        } else if (ContainerRuntime.UNAVAILABLE.name().equalsIgnoreCase(runtime)) {
            return ContainerRuntime.UNAVAILABLE;
        } else {
            log.warnf("System property %s contains an unknown value %s. Ignoring it.",
                    CONTAINER_RUNTIME_SYS_PROP, runtime);
            return null;
        }
    }

    private static void storeConfig(ContainerRuntime containerRuntime) {
        System.setProperty(CONTAINER_RUNTIME_SYS_PROP, containerRuntime.name());
    }

    private static String getVersionOutputFor(ContainerRuntime containerRuntime) {
        Process versionProcess = null;
        try {
            final ProcessBuilder pb = new ProcessBuilder(containerRuntime.getExecutableName(), "--version")
                    .redirectErrorStream(true);
            versionProcess = pb.start();
            versionProcess.waitFor();
            return new String(versionProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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

    private static boolean getRootlessStateFor(ContainerRuntime containerRuntime) {
        Process rootlessProcess = null;
        ProcessBuilder pb = null;
        try {
            pb = new ProcessBuilder(containerRuntime.getExecutableName(), "info").redirectErrorStream(true);
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
                if (exitCode != 0) {
                    log.debugf("Command \"%s\" output: %s", String.join(" ", pb.command()),
                            bufferedReader.lines().collect(Collectors.joining(System.lineSeparator())));
                    return false;
                } else {
                    final Predicate<String> stringPredicate;
                    // Docker includes just "rootless" under SecurityOptions, while podman includes "rootless: <boolean>"
                    if (containerRuntime == ContainerRuntime.DOCKER) {
                        stringPredicate = line -> line.trim().equals("rootless");
                    } else {
                        stringPredicate = line -> line.trim().equals("rootless: true");
                    }
                    return bufferedReader.lines().anyMatch(stringPredicate);
                }
            }
        } catch (IOException | InterruptedException e) {
            // If an exception is thrown in the process, assume we are not running rootless (default docker installation)
            log.debugf(e, "Failure to read info output from %s", String.join(" ", pb.command()));
            return false;
        } finally {
            if (rootlessProcess != null) {
                rootlessProcess.destroy();
            }
        }
    }

    /**
     * Supported Container runtimes
     */
    public enum ContainerRuntime {
        DOCKER,
        PODMAN,
        UNAVAILABLE;

        private Boolean rootless;

        public String getExecutableName() {
            return this.name().toLowerCase();
        }

        public boolean isRootless() {
            if (rootless != null) {
                return rootless;
            } else {
                if (this != ContainerRuntime.UNAVAILABLE) {
                    rootless = getRootlessStateFor(this);
                } else {
                    throw new IllegalStateException("No container runtime was found. "
                            + "Make sure you have either Docker or Podman installed in your environment.");
                }
            }
            return rootless;
        }
    }
}
