package io.quarkus.test.common;

import java.nio.file.Path;

/**
 * Implemented by artifact launchers that resolve a unique, per-instance log file.
 * Allows JUnit extensions and test callbacks to discover the exact log file
 * used by this launcher instance, which is particularly important when
 * {@code maven-failsafe-plugin} is configured with {@code forkCount > 1}.
 *
 * <p>
 * {@link io.quarkus.test.junit.QuarkusIntegrationTestExtension} publishes the resolved path
 * as the {@code quarkus.test.log.file.path} system property after the launcher has started,
 * for simpler access from test methods.
 * </p>
 */
public interface LogPathProvider {

    /**
     * Returns the path of the log file used by this launcher instance.
     *
     * @return the log file path, or {@code null} if the launcher has not been started yet
     */
    Path logFilePath();
}
