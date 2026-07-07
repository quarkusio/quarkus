package io.quarkus.test.common;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Launches a packaged Quarkus JAR for integration tests.
 * <p>
 * The test framework uses a custom implementation discovered through {@link java.util.ServiceLoader}, when present,
 * and otherwise uses {@link DefaultJarLauncher}.
 */
public interface JarArtifactLauncher extends ArtifactLauncher<JarArtifactLauncher.JarInitContext> {

    /**
     * Initialization values supplied to a JAR launcher.
     */
    interface JarInitContext extends InitContext {

        /**
         * @return the packaged JAR to launch
         */
        Path jarPath();

        /**
         * Returns additional JVM arguments for startup-archive recording during the integration-test run.
         * <p>
         * An empty list means that no recording arguments are required.
         *
         * @return the recording JVM arguments
         */
        List<String> recordingArgs();

        /**
         * Returns the JVM arguments for optional processing after the integration-test process exits.
         * <p>
         * The launcher prepends the Java executable and configured JVM arguments, then appends runtime system
         * properties, {@code -jar}, the JAR path, and the original program arguments. An empty list means that no
         * post-close process is required.
         *
         * @return the post-close JVM arguments
         */
        List<String> postCloseCommand();

        /**
         * @return the result file or directory to report after archive processing, or empty when none is expected
         */
        Optional<Path> aotResultPath();

        /**
         * @return a human-readable description used when reporting {@link #aotResultPath()}
         */
        String aotResultDescription();

        /**
         * Returns the explicit typed training request supplied by build-tool metadata.
         * <p>
         * The default is empty so existing custom launcher contexts retain their previous behavior.
         *
         * @return the explicit startup-archive training request, when present
         */
        default Optional<JvmStartupArchiveTraining> startupArchiveTraining() {
            return Optional.empty();
        }
    }
}
