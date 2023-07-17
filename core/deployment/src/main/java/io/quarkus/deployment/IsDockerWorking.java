
package io.quarkus.deployment;

import static io.quarkus.runtime.util.ContainerRuntimeUtil.ContainerRuntime.UNAVAILABLE;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.runtime.util.ContainerRuntimeUtil;

public class IsDockerWorking implements BooleanSupplier {

    private static final Logger LOGGER = Logger.getLogger(IsDockerWorking.class.getName());
    public static final int DOCKER_HOST_CHECK_TIMEOUT = 1000;

    private final List<Strategy> strategies;

    public IsDockerWorking() {
        this(false);
    }

    public IsDockerWorking(boolean silent) {
        this.strategies = List.of(new TestContainersStrategy(silent), new DockerHostStrategy(), new DockerBinaryStrategy());
    }

    @Override
    public boolean getAsBoolean() {
        for (Strategy strategy : strategies) {
            LOGGER.debugf("Checking Docker Environment using strategy %s", strategy.getClass().getName());
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
            // Testcontainers uses the Unreliables library to test if docker is started
            // this runs in threads that start with 'ducttape'
            StartupLogCompressor compressor = new StartupLogCompressor("Checking Docker Environment", Optional.empty(), null,
                    (s) -> s.getName().startsWith("ducttape"));
            try {
                Class<?> dockerClientFactoryClass = Thread.currentThread().getContextClassLoader()
                        .loadClass("org.testcontainers.DockerClientFactory");
                Object dockerClientFactoryInstance = dockerClientFactoryClass.getMethod("instance").invoke(null);

                Class<?> configurationClass = Thread.currentThread().getContextClassLoader()
                        .loadClass("org.testcontainers.utility.TestcontainersConfiguration");
                Object configurationInstance = configurationClass.getMethod("getInstance").invoke(null);
                String oldReusePropertyValue = (String) configurationClass
                        .getMethod("getEnvVarOrUserProperty", String.class, String.class)
                        .invoke(configurationInstance, "testcontainers.reuse.enable", "false"); // use the default provided in TestcontainersConfiguration#environmentSupportsReuse
                Method updateUserConfigMethod = configurationClass.getMethod("updateUserConfig", String.class, String.class);
                // this will ensure that testcontainers does not start ryuk - see https://github.com/quarkusio/quarkus/issues/25852 for why this is important
                updateUserConfigMethod.invoke(configurationInstance, "testcontainers.reuse.enable", "true");

                boolean isAvailable = (boolean) dockerClientFactoryClass.getMethod("isDockerAvailable")
                        .invoke(dockerClientFactoryInstance);
                if (!isAvailable) {
                    compressor.closeAndDumpCaptured();
                }

                // restore the previous value
                updateUserConfigMethod.invoke(configurationInstance, "testcontainers.reuse.enable", oldReusePropertyValue);
                return isAvailable ? Result.AVAILABLE : Result.UNAVAILABLE;
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                if (!silent) {
                    compressor.closeAndDumpCaptured();
                    LOGGER.debug("Unable to use Testcontainers to determine if Docker is working", e);
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

        private static final String UNIX_SCHEME = "unix";

        @Override
        public Result get() {
            String dockerHost = System.getenv("DOCKER_HOST");

            if (dockerHost == null) {
                return Result.UNKNOWN;
            }

            try {
                URI dockerHostUri = new URI(dockerHost);

                if (UNIX_SCHEME.equals(dockerHostUri.getScheme())) {
                    // Java 11 does not support connecting to Unix sockets so for now let's use a naive approach
                    Path dockerSocketPath = Path.of(dockerHostUri.getPath());

                    if (Files.isWritable(dockerSocketPath)) {
                        return Result.AVAILABLE;
                    } else {
                        LOGGER.warnf(
                                "Unix socket defined in DOCKER_HOST %s is not writable, make sure Docker is running on the specified host",
                                dockerHost);
                    }
                } else {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(dockerHostUri.getHost(), dockerHostUri.getPort()),
                                DOCKER_HOST_CHECK_TIMEOUT);
                        return Result.AVAILABLE;
                    } catch (IOException e) {
                        LOGGER.warnf(
                                "Unable to connect to DOCKER_HOST URI %s, make sure Docker is running on the specified host",
                                dockerHost);
                    }
                }
            } catch (URISyntaxException | IllegalArgumentException e) {
                LOGGER.warnf("Unable to parse DOCKER_HOST URI %s, it will be ignored for working Docker detection",
                        dockerHost);
            }

            return Result.UNKNOWN;
        }
    }

    private static class DockerBinaryStrategy implements Strategy {

        @Override
        public Result get() {
            if (ContainerRuntimeUtil.detectContainerRuntime(false) != UNAVAILABLE) {
                return Result.AVAILABLE;
            } else {
                return Result.UNKNOWN;
            }
        }

    }

    private enum Result {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN
    }
}
