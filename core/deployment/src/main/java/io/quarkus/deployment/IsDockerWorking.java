
package io.quarkus.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.util.ExecUtil;

public class IsDockerWorking implements BooleanSupplier {

    private static final Logger LOGGER = Logger.getLogger(IsDockerWorking.class.getName());

    private final List<Strategy> strategies;

    public IsDockerWorking() {
        this(false);
    }

    public IsDockerWorking(boolean silent) {
        this.strategies = List.of(new TestContainersStrategy(silent), new DockerHostStrategy(),
                new DockerBinaryStrategy(silent));
    }

    @Override
    public boolean getAsBoolean() {
        for (Strategy strategy : strategies) {
            Result result = strategy.get();
            if (result == Result.AVAILABLE) {
                return true;
            }
        }
        return false;
    }

    public static class IsDockerRunningSilent extends IsDockerWorking {
        public IsDockerRunningSilent() {
            super(true);
        }
    }

    private interface Strategy extends Supplier<Result> {

    }

    /**
     * Delegates the check to testcontainers (if the latter is on the classpath)
     */
    private static class TestContainersStrategy implements Strategy {

        private final boolean silent;

        private TestContainersStrategy(boolean silent) {
            this.silent = silent;
        }

        @Override
        public Result get() {
            StartupLogCompressor compressor = new StartupLogCompressor("Checking Docker Environment", Optional.empty(), null);
            try {
                Class<?> dockerClientFactoryClass = Thread.currentThread().getContextClassLoader()
                        .loadClass("org.testcontainers.DockerClientFactory");
                Object dockerClientFactoryInstance = dockerClientFactoryClass.getMethod("instance").invoke(null);
                boolean isAvailable = (boolean) dockerClientFactoryClass.getMethod("isDockerAvailable")
                        .invoke(dockerClientFactoryInstance);
                return isAvailable ? Result.AVAILABLE : Result.UNAVAILABLE;
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                if (!silent) {
                    compressor.closeAndDumpCaptured();
                    LOGGER.debug("Unable to use testcontainers to determine if Docker is working", e);
                }
                return Result.UNKNOWN;
            } finally {
                compressor.close();
            }
        }
    }

    /**
     * Detection using a remote host socket
     * We don't want to pull in the docker API here, so we just see if the DOCKER_HOST is set
     * and if we can connect to it.
     * We can't actually verify it is docker listening on the other end.
     * Furthermore, this does not support Unix Sockets
     */
    private static class DockerHostStrategy implements Strategy {

        @Override
        public Result get() {

            String dockerHost = System.getenv("DOCKER_HOST");
            if (dockerHost != null && !dockerHost.startsWith("unix:")) {
                try {
                    URI url = new URI(dockerHost);
                    try (Socket s = new Socket(url.getHost(), url.getPort())) {
                        return Result.AVAILABLE;
                    } catch (IOException e) {
                        LOGGER.warnf(
                                "Unable to connect to DOCKER_HOST URI %s, make sure docker is running on the specified host",
                                dockerHost);
                    }
                } catch (URISyntaxException e) {
                    LOGGER.warnf("Unable to parse DOCKER_HOST URI %s, it will be ignored for working docker detection",
                            dockerHost);
                }
            }
            return Result.UNKNOWN;
        }
    }

    private static class DockerBinaryStrategy implements Strategy {

        private final boolean silent;

        private DockerBinaryStrategy(boolean silent) {
            this.silent = silent;
        }

        @Override
        public Result get() {
            try {
                if (!ExecUtil.execSilent("docker", "-v")) {
                    LOGGER.warn("'docker -v' returned an error code. Make sure your Docker binary is correct");
                    return Result.UNKNOWN;
                }
            } catch (Exception e) {
                LOGGER.warnf("No Docker binary found or general error: %s", e);
                return Result.UNKNOWN;
            }

            try {
                OutputFilter filter = new OutputFilter();
                if (ExecUtil.exec(new File("."), filter, "docker", "version", "--format", "'{{.Server.Version}}'")) {
                    LOGGER.debugf("Docker daemon found. Version: %s", filter.getOutput());
                    return Result.AVAILABLE;
                } else {
                    if (!silent) {
                        LOGGER.warn("Could not determine version of Docker daemon");
                    }
                    return Result.UNAVAILABLE;
                }
            } catch (Exception e) {
                LOGGER.warn("Unexpected error occurred while determining Docker daemon version", e);
                return Result.UNKNOWN;
            }
        }

        public static class OutputFilter implements Function<InputStream, Runnable> {
            private final StringBuilder builder = new StringBuilder();

            @Override
            public Runnable apply(InputStream is) {
                return () -> {

                    try (InputStreamReader isr = new InputStreamReader(is);
                            BufferedReader reader = new BufferedReader(isr)) {

                        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                            builder.append(line);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading stream.", e);
                    }
                };
            }

            public String getOutput() {
                return builder.toString();
            }
        }
    }

    private enum Result {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN
    }
}
