package io.quarkus.test.common;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * If an implementation of this class is found using the ServiceLoader mechanism, then it is used.
 * Otherwise {@link DefaultJarLauncher} is used
 */
public interface JarArtifactLauncher extends ArtifactLauncher<JarArtifactLauncher.JarInitContext> {

    interface JarInitContext extends InitContext {

        Path jarPath();

        /**
         * Additional JVM args to add during the integration test run for training/recording.
         * Empty list means no recording.
         * <p>
         * For Leyden AOT: {@code [-XX:AOTMode=record, -XX:AOTConfiguration=<path>, ...]}
         * For OpenJ9 SCC: {@code [-Xshareclasses:name=quarkus-app,cacheDir=
         *
        <dir>
         * ]}
         */
        List<String> recordingArgs();

        /**
         * Base command to run after the test process exits.
         * The launcher will prepend the java binary and argLine, then append runtime system properties,
         * {@code -jar}, and jar path before executing. Empty list means no post-close processing.
         * <p>
         * For Leyden AOT: {@code [-XX:AOTMode=create, -XX:AOTConfiguration=..., -XX:AOTCache=...]}
         * For Semeru SCC: empty (cache is populated during the test run).
         */
        List<String> postCloseCommand();

        /**
         * Path to the AOT result file or directory that the user should reference when running the application.
         * Used for logging purposes after the AOT process completes.
         * Empty when AOT is not enabled.
         */
        Optional<Path> aotResultPath();

        /**
         * Human-readable description of the AOT result (e.g. "AOT cache", "SCC cache").
         * Used in log messages to describe what was created.
         */
        String aotResultDescription();
    }
}
