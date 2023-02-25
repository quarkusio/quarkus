package io.quarkus.runtime.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

public final class ContainerRuntimeUtil {

    private static final Logger log = Logger.getLogger(ContainerRuntimeUtil.class);

    private ContainerRuntimeUtil() {
    }

    /**
     * @return {@link ContainerRuntime#DOCKER} if it's available, or {@link ContainerRuntime#PODMAN}
     *         if the podman
     *         executable exists in the environment or if the docker executable is an alias to podman
     * @throws IllegalStateException if no container runtime was found to build the image
     */
    public static ContainerRuntime detectContainerRuntime() {
        // Docker version 19.03.14, build 5eb3275d40
        String dockerVersionOutput = getVersionOutputFor(ContainerRuntime.DOCKER);
        boolean dockerAvailable = dockerVersionOutput.contains("Docker version");
        // Check if Podman is installed
        // podman version 2.1.1
        String podmanVersionOutput = getVersionOutputFor(ContainerRuntime.PODMAN);
        boolean podmanAvailable = podmanVersionOutput.startsWith("podman version");

        final String executable = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.native.container-runtime", String.class).orElse(null);
        if (executable != null) {
            if (executable.trim().equalsIgnoreCase("docker") && dockerAvailable) {
                return ContainerRuntime.DOCKER;
            } else if (executable.trim().equalsIgnoreCase("podman") && podmanAvailable) {
                return ContainerRuntime.PODMAN;
            } else {
                log.warn("quarkus.native.container-runtime config property must be set to either podman or docker " +
                        "and the executable must be available. Ignoring it.");
            }
        }

        if (dockerAvailable) {
            // Check if "docker" is an alias to "podman"
            if (dockerVersionOutput.equals(podmanVersionOutput)) {
                return ContainerRuntime.PODMAN;
            }
            return ContainerRuntime.DOCKER;
        } else if (podmanAvailable) {
            return ContainerRuntime.PODMAN;
        } else {
            throw new IllegalStateException("No container runtime was found to. "
                    + "Make sure you have Docker or Podman installed in your environment.");
        }
    }

    private static String getVersionOutputFor(ContainerRuntime containerRuntime) {
        Process versionProcess = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(containerRuntime.getExecutableName(), "--version")
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
        try {
            ProcessBuilder pb = new ProcessBuilder(containerRuntime.getExecutableName(), "info")
                    .redirectErrorStream(true);
            rootlessProcess = pb.start();
            int exitCode = rootlessProcess.waitFor();
            if (exitCode != 0) {
                log.warnf("Command \"%s\" exited with error code %d. " +
                        "Rootless container runtime detection might not be reliable.",
                        containerRuntime.getExecutableName(), exitCode);
            }
            try (InputStream inputStream = rootlessProcess.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                Predicate<String> stringPredicate;
                // Docker includes just "rootless" under SecurityOptions, while podman includes "rootless: <boolean>"
                if (containerRuntime == ContainerRuntime.DOCKER) {
                    stringPredicate = line -> line.trim().equals("rootless");
                } else {
                    stringPredicate = line -> line.trim().equals("rootless: true");
                }
                return bufferedReader.lines().anyMatch(stringPredicate);
            }
        } catch (IOException | InterruptedException e) {
            // If an exception is thrown in the process, assume we are not running rootless (default docker installation)
            log.debugf(e, "Failure to read info output from %s", containerRuntime.getExecutableName());
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
        PODMAN;

        private final boolean rootless;

        ContainerRuntime() {
            this.rootless = getRootlessStateFor(this);
        }

        public String getExecutableName() {
            return this.name().toLowerCase();
        }

        public boolean isRootless() {
            return rootless;
        }
    }
}
