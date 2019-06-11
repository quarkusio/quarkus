package io.quarkus.creator.phase.runnerjar;

import java.nio.file.Path;

/**
 *
 * @author Alexey Loubyansky
 */
public interface RunnerJarOutcome {

    Path getRunnerJar();

    Path getLibDir();

    Path getOriginalJar();
}
