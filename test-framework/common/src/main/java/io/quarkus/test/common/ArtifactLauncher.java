package io.quarkus.test.common;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.app.CuratedApplication;

/**
 * Launches a packaged Quarkus application for integration tests.
 *
 * @param <T> the launcher-specific initialization context
 */
public interface ArtifactLauncher<T extends ArtifactLauncher.InitContext> extends Closeable {

    /**
     * Initializes this launcher before it is started.
     *
     * @param t the launcher configuration
     */
    void init(T t);

    /**
     * Starts the application and waits until its listening addresses are available.
     *
     * @return the application listening addresses
     * @throws IOException if the application cannot be started
     */
    ListeningAddresses start() throws IOException;

    /**
     * Runs the packaged application to completion.
     *
     * @param args application arguments
     * @return the exit status and captured output
     */
    LaunchResult runToCompletion(String[] args);

    /**
     * Adds system properties to the launched application.
     *
     * @param systemProps properties to pass to the application process
     */
    void includeAsSysProps(Map<String, String> systemProps);

    /**
     * Values shared by all artifact launcher initialization contexts.
     */
    interface InitContext {

        /**
         * @return the HTTP test port
         */
        int httpPort();

        /**
         * @return the HTTPS test port
         */
        int httpsPort();

        /**
         * @return the maximum duration to wait for application startup
         */
        Duration waitTime();

        /**
         * @return the graceful application shutdown timeout
         */
        Duration shutdownTimeout();

        /**
         * @return the Quarkus integration-test configuration profile name
         */
        String testProfile();

        /**
         * @return additional arguments passed to the launching runtime
         */
        List<String> argLine();

        /**
         * Returns additional environment variables for the launched process.
         * <p>
         * The launcher also inherits the environment of the current process.
         *
         * @return additional environment variables
         */
        Map<String, String> env();

        /**
         * @return the Dev Services values and resources associated with this launch
         */
        ArtifactLauncher.InitContext.DevServicesLaunchResult getDevServicesLaunchResult();

        /**
         * Dev Services configuration and resources associated with an integration-test launch.
         */
        interface DevServicesLaunchResult extends AutoCloseable {

            /**
             * @return configuration properties contributed by Dev Services
             */
            Map<String, String> properties();

            /**
             * @return the container network identifier, or {@code null} when no dedicated network is used
             */
            String networkId();

            /**
             * @return whether this result owns the lifecycle of the container network
             */
            boolean manageNetwork();

            /**
             * @return the curated application that owns the Dev Services lifecycle
             */
            CuratedApplication getCuratedApplication();

            /**
             * Closes the curated application associated with this result.
             */
            void close();
        }
    }

    /**
     * The result of running an application process to completion.
     */
    class LaunchResult {
        final int statusCode;
        final byte[] output;
        final byte[] stderror;

        /**
         * @param statusCode the application process exit code
         * @param output the captured standard output
         * @param stderror the captured standard error
         */
        public LaunchResult(int statusCode, byte[] output, byte[] stderror) {
            this.statusCode = statusCode;
            this.output = output;
            this.stderror = stderror;
        }

        /**
         * @return the application process exit code
         */
        public int getStatusCode() {
            return statusCode;
        }

        /**
         * @return the captured standard output
         */
        public byte[] getOutput() {
            return output;
        }

        /**
         * @return the captured standard error
         */
        public byte[] getStderror() {
            return stderror;
        }
    }
}
